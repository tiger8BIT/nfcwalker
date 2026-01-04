package ge.tiger8bit.service

import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.domain.CheckpointSubCheck
import ge.tiger8bit.dto.CheckpointResponse
import ge.tiger8bit.dto.CreateCheckpointRequest
import ge.tiger8bit.dto.SubCheckResponse
import ge.tiger8bit.dto.UpdateCheckpointRequest
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.CheckpointRepository
import ge.tiger8bit.repository.CheckpointSubCheckRepository
import ge.tiger8bit.repository.PatrolRouteCheckpointRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.util.*

@Singleton
open class CheckpointService(
    private val checkpointRepository: CheckpointRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    private val checkpointSubCheckRepository: CheckpointSubCheckRepository,
    private val accessService: AccessService
) {
    private val logger = getLogger()

    @Transactional
    open fun create(request: CreateCheckpointRequest, userId: UUID): CheckpointResponse {
        accessService.ensureBossOrAppOwner(userId, request.organizationId)

        logger.info("Creating checkpoint: code={}, orgId={}, siteId={}", request.code, request.organizationId, request.siteId)

        val checkpoint = checkpointRepository.save(
            Checkpoint(
                organizationId = request.organizationId,
                siteId = request.siteId,
                code = request.code,
                geoLat = request.geoLat,
                geoLon = request.geoLon,
                radiusM = request.radiusM,
                label = request.label,
                detailsConfig = request.detailsConfig
            )
        )

        val savedSubChecks = request.subChecks?.map { subRequest ->
            checkpointSubCheckRepository.save(
                CheckpointSubCheck(
                    checkpointId = checkpoint.id!!,
                    label = subRequest.label,
                    description = subRequest.description,
                    requirePhoto = subRequest.requirePhoto,
                    allowNotes = subRequest.allowNotes
                )
            )
        } ?: emptyList()

        logger.info("Checkpoint created: id={}, code={}, subChecks={}", checkpoint.id, checkpoint.code, savedSubChecks.size)
        return checkpoint.toResponse(savedSubChecks)
    }

    @Transactional
    open fun update(id: UUID, request: UpdateCheckpointRequest, userId: UUID): CheckpointResponse {
        val checkpoint = checkpointRepository.findById(id).orElseThrow {
            HttpStatusException(HttpStatus.NOT_FOUND, "Checkpoint not found")
        }
        accessService.ensureBossOrAppOwner(userId, checkpoint.organizationId)

        logger.info("Updating checkpoint: id={}", id)

        request.label?.let { checkpoint.label = it }
        request.geoLat?.let { checkpoint.geoLat = it }
        request.geoLon?.let { checkpoint.geoLon = it }
        request.radiusM?.let { checkpoint.radiusM = it }
        request.detailsConfig?.let { checkpoint.detailsConfig = it }

        checkpointRepository.update(checkpoint)

        // If sub-checks are provided, replace existing ones
        val savedSubChecks = if (request.subChecks != null) {
            checkpointSubCheckRepository.deleteByCheckpointId(id)
            request.subChecks.map { subRequest ->
                checkpointSubCheckRepository.save(
                    CheckpointSubCheck(
                        checkpointId = checkpoint.id!!,
                        label = subRequest.label,
                        description = subRequest.description,
                        requirePhoto = subRequest.requirePhoto,
                        allowNotes = subRequest.allowNotes
                    )
                )
            }
        } else {
            checkpointSubCheckRepository.findByCheckpointId(id)
        }

        logger.info("Checkpoint updated: id={}", id)
        return checkpoint.toResponse(savedSubChecks)
    }

    fun findBySiteId(siteId: UUID, userId: UUID): List<CheckpointResponse> {
        val checkpoints = checkpointRepository.findBySiteId(siteId)
        if (checkpoints.isNotEmpty()) {
            accessService.ensureBossOrAppOwner(userId, checkpoints.first().organizationId)
        }
        return checkpoints.map { cp ->
            val subChecks = checkpointSubCheckRepository.findByCheckpointId(cp.id!!)
            cp.toResponse(subChecks)
        }
    }

    fun findById(id: UUID): Checkpoint? {
        return checkpointRepository.findById(id).orElse(null)
    }

    fun findByCode(code: String): Checkpoint? {
        return checkpointRepository.findByCode(code).orElse(null)
    }

    @Transactional
    open fun delete(id: UUID, userId: UUID): Boolean {
        val checkpoint = checkpointRepository.findById(id).orElseThrow {
            HttpStatusException(HttpStatus.NOT_FOUND, "Checkpoint not found")
        }
        accessService.ensureBossOrAppOwner(userId, checkpoint.organizationId)

        logger.info("Deleting checkpoint: {}", id)

        // First delete all route checkpoint associations
        patrolRouteCheckpointRepository.deleteByCheckpointId(id)

        // Delete sub-checks
        checkpointSubCheckRepository.deleteByCheckpointId(id)

        logger.info("Deleted associations and sub-checks for checkpoint: {}", id)

        // Then delete the checkpoint itself
        checkpointRepository.deleteById(id)
        logger.info("Checkpoint deleted: {}", id)

        return true
    }

    private fun Checkpoint.toResponse(subChecks: List<CheckpointSubCheck> = emptyList()) = CheckpointResponse(
        id = id!!,
        organizationId = organizationId,
        siteId = siteId,
        code = code,
        geoLat = geoLat,
        geoLon = geoLon,
        radiusM = radiusM,
        label = label,
        detailsConfig = detailsConfig,
        subChecks = subChecks.map { it.toResponse() }
    )

    private fun CheckpointSubCheck.toResponse() = SubCheckResponse(
        id = id!!,
        label = label,
        description = description,
        requirePhoto = requirePhoto,
        allowNotes = allowNotes
    )
}

