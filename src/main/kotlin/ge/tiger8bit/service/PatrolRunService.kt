package ge.tiger8bit.service

import ge.tiger8bit.domain.PatrolRun
import ge.tiger8bit.dto.CreatePatrolRunRequest
import ge.tiger8bit.dto.PatrolRunResponse
import ge.tiger8bit.dto.PatrolRunStatus
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.PatrolRunRepository
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*

@Singleton
open class PatrolRunService(
    private val patrolRunRepository: PatrolRunRepository,
    private val accessService: AccessService
) {
    private val logger = getLogger()

    @Transactional
    open fun createRun(request: CreatePatrolRunRequest, userId: UUID): PatrolRunResponse {
        accessService.ensureBossOrAppOwner(userId, request.organizationId)

        logger.info("Creating patrol run for route: {}, org: {}", request.routeId, request.organizationId)

        val plannedStart = request.plannedStart ?: Instant.now()
        val plannedEnd = request.plannedEnd ?: plannedStart.plus(Duration.ofHours(4))

        val run = patrolRunRepository.save(
            PatrolRun(
                routeId = request.routeId,
                organizationId = request.organizationId,
                plannedStart = plannedStart,
                plannedEnd = plannedEnd,
                status = PatrolRunStatus.IN_PROGRESS // Start it immediately for now
            )
        )

        logger.info("Patrol run created: id={}, status={}", run.id, run.status)
        return run.toResponse()
    }

    fun findById(id: UUID): PatrolRun? {
        return patrolRunRepository.findById(id).orElse(null)
    }

    private fun PatrolRun.toResponse() = PatrolRunResponse(
        id = id!!,
        routeId = routeId,
        organizationId = organizationId,
        status = status,
        plannedStart = plannedStart,
        plannedEnd = plannedEnd
    )
}
