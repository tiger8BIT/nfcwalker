package ge.tiger8bit

import ge.tiger8bit.domain.*
import ge.tiger8bit.repository.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object TestFixtures {
    fun cleanupChallengeUsed(challengeUsedRepository: ChallengeUsedRepository) {
        try {
            challengeUsedRepository.deleteAll()
        } catch (_: Exception) {
            // best-effort
        }
    }

    fun seedOrgAndSite(
        organizationRepository: OrganizationRepository,
        siteRepository: SiteRepository,
        orgName: String = "Test Org ${UUID.randomUUID()}",
        siteName: String = "Test Site ${UUID.randomUUID()}"
    ): Pair<Organization, Site> {
        val org = organizationRepository.save(Organization(name = orgName))
        organizationRepository.flush()
        val site = siteRepository.save(Site(organizationId = org.id!!, name = siteName))
        siteRepository.flush()
        return org to site
    }

    fun createRoute(
        patrolRouteRepository: PatrolRouteRepository,
        organizationId: UUID,
        siteId: UUID,
        name: String = "Route ${UUID.randomUUID()}"
    ): PatrolRoute {
        val route = patrolRouteRepository.save(
            PatrolRoute(organizationId = organizationId, siteId = siteId, name = name)
        )
        patrolRouteRepository.flush()
        return route
    }


    fun createPatrolRun(
        patrolRunRepository: PatrolRunRepository,
        routeId: UUID,
        organizationId: UUID,
        status: String = "in_progress"
    ): PatrolRun {
        val run = patrolRunRepository.save(
            PatrolRun(
                routeId = routeId,
                organizationId = organizationId,
                plannedStart = Instant.now(),
                plannedEnd = Instant.now().plusSeconds(7200),
                status = status
            )
        )
        patrolRunRepository.flush()
        return run
    }
}

