package ge.tiger8bit

import ge.tiger8bit.domain.*
import ge.tiger8bit.repository.*
import io.micronaut.context.BeanContext
import java.time.Instant
import java.util.*

object TestFixtures {
    private lateinit var beanContext: BeanContext

    fun init(context: BeanContext) {
        beanContext = context
    }

    private val organizationRepository: OrganizationRepository
        get() = beanContext.getBean(OrganizationRepository::class.java)
    private val siteRepository: SiteRepository
        get() = beanContext.getBean(SiteRepository::class.java)
    private val userRepository: UserRepository
        get() = beanContext.getBean(UserRepository::class.java)
    private val userRoleRepository: UserRoleRepository
        get() = beanContext.getBean(UserRoleRepository::class.java)
    private val deviceRepository: DeviceRepository
        get() = beanContext.getBean(DeviceRepository::class.java)
    private val patrolRouteRepository: PatrolRouteRepository
        get() = beanContext.getBean(PatrolRouteRepository::class.java)
    private val patrolRunRepository: PatrolRunRepository
        get() = beanContext.getBean(PatrolRunRepository::class.java)

    fun createOrganization(name: String = "Test Org ${UUID.randomUUID()}"): Organization {
        val org = organizationRepository.save(Organization(name = name))
        organizationRepository.flush()
        return org
    }

    fun cleanupChallengeUsed(challengeUsedRepository: ChallengeUsedRepository) {
        try {
            challengeUsedRepository.deleteAll()
        } catch (_: Exception) {
        }
    }

    fun seedOrgAndSite(
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

    fun createUserWithRole(
        organizationId: UUID,
        role: Role,
        email: String = "user-${UUID.randomUUID()}@test.com",
        name: String = "Test User"
    ): Pair<User, UserRole> {
        val user = userRepository.save(
            User(
                email = email,
                name = name,
                googleId = "google-${UUID.randomUUID()}"
            )
        )
        userRepository.flush()

        val userRole = userRoleRepository.save(
            UserRole(
                userId = user.id!!,
                organizationId = organizationId,
                role = role
            )
        )
        userRoleRepository.flush()

        return user to userRole
    }

    fun createUser(
        email: String = "user-${UUID.randomUUID()}@test.com",
        name: String = "Test User",
        googleId: String? = "google-${UUID.randomUUID()}"
    ): User {
        val user = userRepository.save(
            User(
                email = email,
                name = name,
                googleId = googleId
            )
        )
        userRepository.flush()
        return user
    }

    fun createDevice(
        userId: UUID,
        organizationId: UUID,
        deviceId: String = "device-${UUID.randomUUID()}",
        metadata: String? = """{"name":"Test Device","os":"Android","type":"mobile"}"""
    ): Device {
        val device = deviceRepository.save(
            Device(
                userId = userId,
                organizationId = organizationId,
                deviceId = deviceId,
                metadata = metadata
            )
        )
        deviceRepository.flush()
        return device
    }
}
