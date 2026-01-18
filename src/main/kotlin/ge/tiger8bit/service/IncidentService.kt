package ge.tiger8bit.service

import ge.tiger8bit.domain.Attachment
import ge.tiger8bit.domain.AttachmentEntityType
import ge.tiger8bit.domain.Incident
import ge.tiger8bit.dto.IncidentCreateRequest
import ge.tiger8bit.dto.IncidentPatchRequest
import ge.tiger8bit.dto.IncidentResponse
import ge.tiger8bit.dto.IncidentStatus
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.AttachmentRepository
import ge.tiger8bit.repository.IncidentRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.CompletedFileUpload
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.*

@Singleton
open class IncidentService(
    private val incidentRepository: IncidentRepository,
    private val accessService: AccessService,
    private val attachmentRepository: AttachmentRepository,
    private val fileManagementService: Optional<FileManagementService>
) {
    private val logger = getLogger()

    @Transactional
    open fun createIncident(request: IncidentCreateRequest, userId: UUID, photos: List<CompletedFileUpload>? = null): IncidentResponse {
        val orgId = request.organizationId ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "OrganizationId is required")
        val siteId = request.siteId ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "SiteId is required")

        accessService.ensureWorkerOrBoss(userId, orgId)

        val incident = incidentRepository.save(
            Incident(
                organizationId = orgId,
                siteId = siteId,
                reportedBy = userId,
                description = request.description,
                severity = request.severity,
                checkpointId = request.checkpointId,
                scanEventId = request.scanEventId
            )
        )

        photos?.let { uploadPhotos(incident.id!!, it) }

        return toResponse(incident)
    }

    @Transactional
    open fun createIncidentInternal(
        orgId: UUID,
        siteId: UUID,
        checkpointId: UUID?,
        scanEventId: UUID?,
        userId: UUID,
        request: IncidentCreateRequest
    ): Incident {
        return incidentRepository.save(
            Incident(
                organizationId = orgId,
                siteId = siteId,
                reportedBy = userId,
                description = request.description,
                severity = request.severity,
                checkpointId = checkpointId,
                scanEventId = scanEventId
            )
        )
    }

    @Transactional
    open fun patchIncident(incidentId: UUID, request: IncidentPatchRequest, userId: UUID): IncidentResponse {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Incident not found") }

        accessService.ensureBossOrAppOwner(userId, incident.organizationId)

        request.description?.let { incident.description = it }
        request.severity?.let { incident.severity = it }
        request.status?.let { incident.status = it }
        incident.updatedAt = Instant.now()

        return toResponse(incidentRepository.update(incident))
    }

    open fun getIncident(incidentId: UUID, userId: UUID): IncidentResponse {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Incident not found") }

        accessService.ensureAnyRoleInOrg(userId, incident.organizationId)
        return toResponse(incident)
    }

    open fun listIncidents(
        organizationId: UUID,
        siteId: UUID?,
        status: IncidentStatus?,
        userId: UUID,
        pageable: io.micronaut.data.model.Pageable
    ): io.micronaut.data.model.Page<IncidentResponse> {
        accessService.ensureAnyRoleInOrg(userId, organizationId)
        val page = incidentRepository.findPaginated(organizationId, siteId, status?.name, pageable)
        return page.map { toResponse(it) }
    }

    @Transactional
    open fun attachToScan(incidentId: UUID, scanEventId: UUID, userId: UUID): IncidentResponse {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Incident not found") }

        accessService.ensureBossOrAppOwner(userId, incident.organizationId)

        incident.scanEventId = scanEventId
        incident.updatedAt = Instant.now()

        return toResponse(incidentRepository.update(incident))
    }

    open fun listIncidentsByScanEvent(scanEventId: UUID, userId: UUID): List<IncidentResponse> {
        val incidents = incidentRepository.findByScanEventId(scanEventId)
        if (incidents.isEmpty()) {
            return emptyList()
        }
        // Check access using first incident's organization
        accessService.ensureAnyRoleInOrg(userId, incidents.first().organizationId)
        return incidents.map { toResponse(it) }
    }

    @Transactional
    open fun deleteIncident(incidentId: UUID, userId: UUID) {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Incident not found") }

        accessService.ensureBossOrAppOwner(userId, incident.organizationId)

        // Delete all attachments
        val attachments = attachmentRepository.findByEntityTypeAndEntityId(AttachmentEntityType.incident, incidentId)
        fileManagementService.ifPresent { fms ->
            attachments.forEach { attachment ->
                try {
                    fms.deleteFile(attachment.filePath)
                } catch (e: Exception) {
                    logger.warn("Failed to delete file: ${attachment.filePath}", e)
                }
            }
        }
        attachmentRepository.deleteAll(attachments)

        // Delete incident
        incidentRepository.delete(incident)
    }

    @Transactional
    open fun addPhotosToIncident(incidentId: UUID, photos: List<CompletedFileUpload>, userId: UUID) {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Incident not found") }

        accessService.ensureWorkerOrBoss(userId, incident.organizationId)

        uploadPhotos(incidentId, photos)
    }

    @Transactional
    open fun deletePhotoFromIncident(incidentId: UUID, photoId: UUID, userId: UUID) {
        val incident = incidentRepository.findById(incidentId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Incident not found") }

        accessService.ensureWorkerOrBoss(userId, incident.organizationId)

        val attachment = attachmentRepository.findById(photoId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Photo not found") }

        if (attachment.entityType != AttachmentEntityType.incident || attachment.entityId != incidentId) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Photo does not belong to this incident")
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

    open fun uploadPhotos(incidentId: UUID, photos: List<CompletedFileUpload>) {
        logger.info("Attaching [${photos.size}] photos to the incident id: [$incidentId]")
        fileManagementService.ifPresent { fms ->
            photos.forEach { photo ->
                val path = "incidents/$incidentId/${UUID.randomUUID()}_${photo.filename}"
                val filePath = fms.uploadFile(photo, path)
                attachmentRepository.save(
                    Attachment(
                        entityType = AttachmentEntityType.incident,
                        entityId = incidentId,
                        filePath = filePath,
                        originalName = photo.filename,
                        contentType = photo.contentType.orElse(null)?.toString(),
                        fileSize = photo.size
                    )
                )
            }
        }
    }

    private fun toResponse(incident: Incident): IncidentResponse {
        return IncidentResponse(
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
}
