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
import java.time.Instant
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
    private val fileManagementService: Optional<FileManagementService>
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

                logger.debug("Creating scan event for checkpoint: {}", checkpointId)
                val scanEvent = patrolScanEventRepository.save(
                    PatrolScanEvent(
                        patrolRunId = patrolRun.id!!,
                        checkpointId = checkpointId,
                        userId = userId,
                        scannedAt = Instant.parse(request.scannedAt),
                        lat = request.lat,
                        lon = request.lon,
                        verdict = ScanVerdict.OK.name.lowercase(),
                        checkStatus = request.checkStatus?.name?.lowercase(),
                        checkNotes = request.checkNotes
                    )
                )

                logger.info("Scan event recorded: id={}, user={}, cp={}", scanEvent.id, userId, checkpointId)

                // Record sub-check results if any
                logger.debug("Sub-check results in request: {}", request.subCheckResults)
                request.subCheckResults?.forEach { subResult ->
                    logger.info("Saving sub-check result: subCheckId={}, status={}", subResult.subCheckId, subResult.status)
                    patrolSubCheckEventRepository.save(
                        PatrolSubCheckEvent(
                            scanEventId = scanEvent.id!!,
                            subCheckId = subResult.subCheckId,
                            status = subResult.status.name.lowercase(),
                            notes = subResult.notes
                        )
                    )
                }

                return FinishScanResponse(
                    eventId = scanEvent.id!!,
                    verdict = ScanVerdict.OK
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

        fileManagementService.ifPresent { fms ->
            photos.forEach { photo ->
                val partName = photo.name

                // Determine entity for attachment: main event or sub-check event
                var attachmentEntityType = "scan_event"
                var attachmentEntityId = eventId

                if (partName.startsWith("photos_")) {
                    val subCheckIdStr = partName.removePrefix("photos_")
                    logger.debug("Mapping photo via partName: {}", partName)
                    try {
                        val subCheckId = UUID.fromString(subCheckIdStr)
                        subCheckEvents[subCheckId]?.let { subEvent ->
                            attachmentEntityType = "sub_check_event"
                            attachmentEntityId = subEvent.id!!
                            logger.debug("Mapped to sub-check event: {}", attachmentEntityId)
                        }
                    } catch (e: IllegalArgumentException) {
                        logger.warn("Invalid subCheckId in part name: {}", subCheckIdStr)
                    }
                } else if (photo.filename.contains("sub_")) {
                    // Alternative mapping using filename prefix: sub_{UUID}.jpg
                    val subCheckIdStr = photo.filename.substringAfter("sub_").substringBefore(".jpg").substringBefore("_")
                    logger.debug("Mapping photo via filename: {}, subCheckIdStr: {}", photo.filename, subCheckIdStr)
                    try {
                        val subCheckId = UUID.fromString(subCheckIdStr)
                        subCheckEvents[subCheckId]?.let { subEvent ->
                            attachmentEntityType = "sub_check_event"
                            attachmentEntityId = subEvent.id!!
                            logger.debug("Mapped to sub-check event: {}", attachmentEntityId)
                        }
                    } catch (e: Exception) {
                        logger.warn("Invalid subCheckId in filename: {}", photo.filename)
                    }
                }

                val path = "scans/$eventId/${UUID.randomUUID()}_${photo.filename}"
                val filePath = fms.uploadFile(photo, path)
                attachmentRepository.save(
                    Attachment(
                        entityType = attachmentEntityType,
                        entityId = attachmentEntityId,
                        filePath = filePath,
                        originalName = photo.filename,
                        contentType = photo.contentType.orElse(null)?.toString(),
                        fileSize = photo.size
                    )
                )
            }
        }

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

    private fun findActivePatrolRun(orgId: UUID): PatrolRun {
        val runs = patrolRunRepository.findAll()
            .filter { it.organizationId == orgId && it.status in listOf("pending", "in_progress") }

        return runs.firstOrNull()
            ?: run {
                logger.warn("No active patrol run for org: {}", orgId)
                throw HttpStatusException(HttpStatus.NOT_FOUND, "No active patrol run found")
            }
    }
}

