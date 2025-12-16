package ge.tiger8bit.service

import ge.tiger8bit.domain.Checkpoint
import ge.tiger8bit.dto.CheckpointResponse
import ge.tiger8bit.dto.CreateCheckpointRequest
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.CheckpointRepository
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
                radiusM = request.radiusM
            )
        )

        logger.info("Checkpoint created: id={}, code={}", checkpoint.id, checkpoint.code)
        return checkpoint.toResponse()
    }

    fun findBySiteId(siteId: UUID, userId: UUID): List<CheckpointResponse> {
        val checkpoints = checkpointRepository.findBySiteId(siteId)
        if (checkpoints.isNotEmpty()) {
            accessService.ensureBossOrAppOwner(userId, checkpoints.first().organizationId)
        }
        return checkpoints.map { it.toResponse() }
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
        logger.info("Deleted route associations for checkpoint: {}", id)

        // Then delete the checkpoint itself
        checkpointRepository.deleteById(id)
        logger.info("Checkpoint deleted: {}", id)

        return true
    }

    private fun Checkpoint.toResponse() = CheckpointResponse(
        id = id!!,
        organizationId = organizationId,
        siteId = siteId,
        code = code,
        geoLat = geoLat,
        geoLon = geoLon,
        radiusM = radiusM
    )
}

