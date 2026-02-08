package ge.tiger8bit.service

import ge.tiger8bit.constants.ChallengeConstants
import ge.tiger8bit.domain.*
import ge.tiger8bit.dto.*
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.*
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.CompletedFileUpload
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.util.*

@Singleton
open class ScanService(
    private val checkpointRepository: CheckpointRepository,
    private val patrolRunRepository: PatrolRunRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    private val patrolScanEventRepository: PatrolScanEventRepository,
    private val checkpointSubCheckRepository: CheckpointSubCheckRepository,
    private val patrolSubCheckEventRepository: PatrolSubCheckEventRepository,
    private val challengeService: ChallengeService,
    private val accessService: AccessService,
    private val attachmentRepository: AttachmentRepository,
    private val fileManagementService: Optional<FileManagementService>,
    private val incidentService: IncidentService,
    private val incidentRepository: IncidentRepository
) {
    private val logger = getLogger()

    @Transactional
    open fun startScan(request: StartScanRequest, userId: UUID): StartScanResponse {
        accessService.ensureWorkerOrBoss(userId, request.organizationId)

        logger.info("Start scan: cpCode={}, device={}, org={}", request.checkpointCode, request.deviceId, request.organizationId)

        val checkpoint = checkpointRepository.findByCode(request.checkpointCode)
            .orElseThrow {
                logger.warn("Checkpoint not found: code={}", request.checkpointCode)
                HttpStatusException(HttpStatus.NOT_FOUND, "Checkpoint not found")
            }

        if (checkpoint.organizationId != request.organizationId) {
            logger.warn("Organization mismatch: expected={}, got={}", checkpoint.organizationId, request.organizationId)
            throw HttpStatusException(HttpStatus.FORBIDDEN, "Organization mismatch")
        }

        val patrolRun = findActivePatrolRun(request.organizationId)

        val routeCheckpoints = patrolRouteCheckpointRepository.findByRouteIdOrderBySeqAsc(patrolRun.routeId)
        val cpInRoute = routeCheckpoints.find { it.checkpointId == checkpoint.id!! }
            ?: run {
                logger.warn("Checkpoint not in route: cpId={}, routeId={}", checkpoint.id, patrolRun.routeId)
                throw HttpStatusException(HttpStatus.BAD_REQUEST, "Checkpoint not in route")
            }

        val challenge = challengeService.issue(
            organizationId = request.organizationId,
            deviceId = request.deviceId,
            checkpointId = checkpoint.id!!
        )

        logger.info("Challenge issued: org={}, device={}, cp={}", request.organizationId, request.deviceId, checkpoint.id)

        val policy = buildScanPolicy(patrolRun, checkpoint, cpInRoute)
        return StartScanResponse(challenge, policy)
    }

    @Transactional
    open fun finishScan(request: FinishScanRequest, userId: UUID): FinishScanResponse {
        val (orgId, deviceId, checkpointId) = parseChallenge(request.challenge)
        accessService.ensureWorkerOrBoss(userId, orgId)

        when (val result = challengeService.validateAndConsume(request.challenge, orgId, deviceId, checkpointId)) {
            is ValidationResult.Invalid -> {
                logger.warn("Challenge validation failed: reason={}", result.reason)
                throw HttpStatusException(HttpStatus.BAD_REQUEST, result.reason)
            }
            is ValidationResult.Replay -> {
                logger.warn("Replay attack detected: org={}, device={}, cp={}", orgId, deviceId, checkpointId)
                throw HttpStatusException(HttpStatus.CONFLICT, result.reason)
            }
            is ValidationResult.Valid -> {
                val patrolRun = findActivePatrolRun(orgId)

                val verdict = if (!request.incidents.isNullOrEmpty() || request.checkStatus == CheckStatus.PROBLEMS_FOUND) {
                    ScanVerdict.WARNING
                } else {
                    ScanVerdict.OK
                }

                logger.debug("Creating scan event for checkpoint: {}", checkpointId)
                val scanEvent = patrolScanEventRepository.save(
                    PatrolScanEvent(
                        patrolRunId = patrolRun.id!!,
                        checkpointId = checkpointId,
                        userId = userId,
                        scannedAt = request.scannedAt,
                        lat = request.lat,
                        lon = request.lon,
                        verdict = verdict,
                        checkStatus = request.checkStatus,
                        checkNotes = request.checkNotes
                    )
                )

                logger.info("Scan event recorded: id={}, user={}, cp={}, verdict={}", scanEvent.id, userId, checkpointId, verdict)

                // Record sub-check results if any
                request.subCheckResults?.forEach { subResult ->
                    logger.info("Saving sub-check result: subCheckId={}, status={}", subResult.subCheckId, subResult.status)
                    patrolSubCheckEventRepository.save(
                        PatrolSubCheckEvent(
                            scanEventId = scanEvent.id!!,
                            subCheckId = subResult.subCheckId,
                            status = subResult.status,
                            notes = subResult.notes
                        )
                    )
                }

                // Create incidents if any
                val checkpoint = checkpointRepository.findById(checkpointId).orElse(null)
                request.incidents?.forEach { incidentRequest ->
                    logger.info("Creating incident during scan: description={}", incidentRequest.description)
                    incidentService.createIncidentInternal(
                        orgId = orgId,
                        siteId = checkpoint?.siteId ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "Checkpoint not found"),
                        checkpointId = checkpointId,
                        scanEventId = scanEvent.id!!,
                        userId = userId,
                        request = incidentRequest
                    )
                }

                return FinishScanResponse(
                    eventId = scanEvent.id!!,
                    verdict = verdict
                )
            }
        }
    }

    @Transactional
    open fun finishScanWithPhotos(
        request: FinishScanRequest,
        photos: List<CompletedFileUpload>,
        userId: UUID
    ): FinishScanResponse {
        val response = finishScan(request, userId)
        val eventId = response.eventId

        // Get created sub-check events to map them by subCheckId
        val subCheckEvents = patrolSubCheckEventRepository.findByScanEventId(eventId)
            .associateBy { it.subCheckId }

        // Get created incidents to map photos (in the same transaction, so they're visible)
        val incidents = incidentRepository.findByScanEventId(eventId)
            .map { incident ->
                IncidentResponse(
                    id = incident.id!!,
                    organizationId = incident.organizationId,
                    siteId = incident.siteId,
                    checkpointId = incident.checkpointId,
                    scanEventId = incident.scanEventId,
                    reportedBy = incident.reportedBy,
                    description = incident.description,
                    severity = incident.severity,
                    status = incident.status,
                    createdAt = incident.createdAt,
                    updatedAt = incident.updatedAt
                )
            }

        fileManagementService.ifPresentOrElse({ fms ->
            logger.info("FileManagementService PRESENT, processing {} photos", photos.size)
            logger.info("SubCheckEvents map: {}", subCheckEvents.mapValues { "eventId=${it.value.id}" })

            photos.forEach { photo ->
                val partName = photo.name

                // Determine entity for attachment: main event, sub-check event, or incident
                var attachmentEntityType = AttachmentEntityType.scan_event
                var attachmentEntityId = eventId

                logger.info("Photo: filename='{}', partName='{}'", photo.filename, partName)

                // Check if photo is for an incident (incidentPhotos_{index})
                if (partName.startsWith("incidentPhotos_")) {
                    val incidentIndexStr = partName.removePrefix("incidentPhotos_")
                    logger.debug("Mapping photo via partName: {}", partName)
                    try {
                        val incidentIndex = incidentIndexStr.toIntOrNull()
                        if (incidentIndex != null && incidentIndex < incidents.size) {
                            attachmentEntityType = AttachmentEntityType.incident
                            attachmentEntityId = incidents[incidentIndex].id
                            logger.debug("Mapped to incident: {}", attachmentEntityId)
                        }
                    } catch (e: Exception) {
                        logger.warn("Invalid incident index in part name: {}", incidentIndexStr)
                    }
                } else if (partName.startsWith("photos_")) {
                    val subCheckIdStr = partName.removePrefix("photos_")
                    logger.debug("Mapping photo via partName: {}", partName)
                    try {
                        val subCheckId = UUID.fromString(subCheckIdStr)
                        subCheckEvents[subCheckId]?.let { subEvent ->
                            attachmentEntityType = AttachmentEntityType.sub_check_event
                            attachmentEntityId = subEvent.id!!
                            logger.debug("Mapped to sub-check event: {}", attachmentEntityId)
                        }
                    } catch (e: IllegalArgumentException) {
                        logger.warn("Invalid subCheckId in part name: {}", subCheckIdStr)
                    }
                } else if (photo.filename.contains("sub_")) {
                    // Alternative mapping using filename prefix: sub_{UUID}_*.jpg or sub_{UUID}.jpg
                    val afterSub = photo.filename.substringAfter("sub_")
                    val subCheckIdStr = if (afterSub.contains("_")) {
                        afterSub.substringBefore("_")  // sub_{UUID}_1.jpg → UUID
                    } else {
                        afterSub.substringBefore(".")  // sub_{UUID}.jpg → UUID
                    }
                    logger.info("Extracted subCheckIdStr: '{}'", subCheckIdStr)
                    try {
                        val subCheckId = UUID.fromString(subCheckIdStr)
                        subCheckEvents[subCheckId]?.let { subEvent ->
                            attachmentEntityType = AttachmentEntityType.sub_check_event
                            attachmentEntityId = subEvent.id!!
                            logger.info("✅ Mapped to sub_check_event: subCheckId={}, subEventId={}", subCheckId, attachmentEntityId)
                        } ?: logger.error(
                            "❌ NO sub-check event for subCheckId={}, available: {}",
                            subCheckId, subCheckEvents.keys
                        )
                    } catch (e: Exception) {
                        logger.error("❌ Failed to parse UUID from '{}': {}", subCheckIdStr, e.message)
                    }
                }

                val path = when (attachmentEntityType) {
                    AttachmentEntityType.incident -> "incidents/$attachmentEntityId/${UUID.randomUUID()}_${photo.filename}"
                    else -> "scans/$eventId/${UUID.randomUUID()}_${photo.filename}"
                }

                logger.info("Uploading: type={}, entityId={}, filename={}", attachmentEntityType, attachmentEntityId, photo.filename)
                val filePath = fms.uploadFile(photo, path)

                val saved = attachmentRepository.save(
                    Attachment(
                        entityType = attachmentEntityType,
                        entityId = attachmentEntityId,
                        filePath = filePath,
                        originalName = photo.filename,
                        contentType = photo.contentType.orElse(null)?.toString(),
                        fileSize = photo.size
                    )
                )
                logger.info("✅ Saved attachment id={}", saved.id)
            }
        }, {
            logger.error("❌❌❌ FileManagementService NOT PRESENT - photos NOT saved!")
        })

        return response
    }

    private fun buildScanPolicy(
        patrolRun: PatrolRun,
        checkpoint: Checkpoint,
        cpInRoute: PatrolRouteCheckpoint
    ): ScanPolicy {
        val geoConstraint = if (checkpoint.geoLat != null && checkpoint.geoLon != null && checkpoint.radiusM != null) {
            GeoConstraint(
                lat = checkpoint.geoLat!!,
                lon = checkpoint.geoLon!!,
                radiusM = checkpoint.radiusM!!
            )
        } else null

        return ScanPolicy(
            runId = patrolRun.id!!,
            checkpointId = checkpoint.id!!,
            order = cpInRoute.seq,
            timeWindow = TimeWindow(
                minOffsetSec = cpInRoute.minOffsetSec,
                maxOffsetSec = cpInRoute.maxOffsetSec
            ),
            geo = geoConstraint
        )
    }

    private fun parseChallenge(jws: String): Triple<UUID, String, UUID> {
        logger.debug("Parsing challenge: '{}'", jws)
        return try {
            val signedJWT = com.nimbusds.jwt.SignedJWT.parse(jws)
            val claims = signedJWT.jwtClaimsSet
            Triple(
                UUID.fromString(claims.getClaim(ChallengeConstants.Claims.ORGANIZATION) as String),
                claims.getClaim(ChallengeConstants.Claims.DEVICE) as String,
                UUID.fromString(claims.getClaim(ChallengeConstants.Claims.CHECKPOINT) as String)
            )
        } catch (e: Exception) {
            logger.warn("Invalid challenge format: {}. Input: '{}'", e.message, jws)
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid challenge format")
        }
    }

    @Transactional
    open fun addPhotosToScanEvent(scanEventId: UUID, photos: List<CompletedFileUpload>, userId: UUID) {
        val scanEvent = patrolScanEventRepository.findById(scanEventId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Scan event not found") }

        val patrolRun = patrolRunRepository.findById(scanEvent.patrolRunId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Patrol run not found") }

        accessService.ensureWorkerOrBoss(userId, patrolRun.organizationId)

        fileManagementService.ifPresent { fms ->
            photos.forEach { photo ->
                val path = "scans/$scanEventId/${UUID.randomUUID()}_${photo.filename}"
                val filePath = fms.uploadFile(photo, path)
                attachmentRepository.save(
                    Attachment(
                        entityType = AttachmentEntityType.scan_event,
                        entityId = scanEventId,
                        filePath = filePath,
                        originalName = photo.filename,
                        contentType = photo.contentType.orElse(null)?.toString(),
                        fileSize = photo.size
                    )
                )
            }
        }
    }

    @Transactional
    open fun deletePhotoFromScanEvent(scanEventId: UUID, photoId: UUID, userId: UUID) {
        val scanEvent = patrolScanEventRepository.findById(scanEventId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Scan event not found") }

        val patrolRun = patrolRunRepository.findById(scanEvent.patrolRunId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Patrol run not found") }

        accessService.ensureWorkerOrBoss(userId, patrolRun.organizationId)

        val attachment = attachmentRepository.findById(photoId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Photo not found") }

        if (attachment.entityType != AttachmentEntityType.scan_event || attachment.entityId != scanEventId) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Photo does not belong to this scan event")
        }

        fileManagementService.ifPresent { fms ->
            try {
                fms.deleteFile(attachment.filePath)
            } catch (e: Exception) {
                logger.warn("Failed to delete file: ${attachment.filePath}", e)
            }
        }

        attachmentRepository.delete(attachment)
    }

    private fun findActivePatrolRun(orgId: UUID): PatrolRun {
        val runs = patrolRunRepository.findByOrganizationIdAndStatusIn(orgId, listOf(PatrolRunStatus.PENDING, PatrolRunStatus.IN_PROGRESS))

        return runs.firstOrNull()
            ?: run {
                logger.warn("No active patrol run for org: {}", orgId)
                throw HttpStatusException(HttpStatus.NOT_FOUND, "No active patrol run found")
            }
    }
}

