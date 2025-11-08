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
        orgName: String = "Test Org",
        siteName: String = "Test Site"
    ): Pair<Organization, Site> {
        val org = organizationRepository.save(Organization(name = orgName))
        organizationRepository.flush()
        val site = siteRepository.save(Site(organizationId = org.id!!, name = siteName))
        siteRepository.flush()
        return org to site
    }

    fun createCheckpoint(
        checkpointRepository: CheckpointRepository,
        organizationId: UUID,
        siteId: UUID,
        code: String = "CP-AUTO-001",
        geoLat: BigDecimal? = null,
        geoLon: BigDecimal? = null,
        radiusM: BigDecimal? = null
    ): Checkpoint {
        val cp = checkpointRepository.save(
            Checkpoint(
                organizationId = organizationId,
                siteId = siteId,
                code = code,
                geoLat = geoLat,
                geoLon = geoLon,
                radiusM = radiusM
            )
        )
        checkpointRepository.flush()
        return cp
    }

    fun createRouteWithTwoCheckpoints(
        organizationRepository: OrganizationRepository,
        siteRepository: SiteRepository,
        checkpointRepository: CheckpointRepository,
        patrolRouteRepository: PatrolRouteRepository,
        patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
        orgName: String = "Route Org",
        siteName: String = "Route Site",
        cp1Code: String = "CP-R1",
        cp2Code: String = "CP-R2"
    ): Pair<PatrolRoute, Pair<Checkpoint, Checkpoint>> {
        val (org, site) = seedOrgAndSite(organizationRepository, siteRepository, orgName, siteName)
        val cp1 = createCheckpoint(checkpointRepository, org.id!!, site.id!!, cp1Code)
        val cp2 = createCheckpoint(checkpointRepository, org.id!!, site.id!!, cp2Code)
        val route = patrolRouteRepository.save(PatrolRoute(organizationId = org.id!!, siteId = site.id!!, name = "Test Route"))
        patrolRouteRepository.flush()
        patrolRouteCheckpointRepository.save(PatrolRouteCheckpoint(routeId = route.id!!, checkpointId = cp1.id!!, seq = 1))
        patrolRouteCheckpointRepository.save(PatrolRouteCheckpoint(routeId = route.id!!, checkpointId = cp2.id!!, seq = 2))
        return route to (cp1 to cp2)
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

