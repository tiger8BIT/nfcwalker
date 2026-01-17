package ge.tiger8bit.spec.common

import ge.tiger8bit.domain.*
import ge.tiger8bit.repository.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Instant
import java.util.*

@Singleton
class TestFixtures @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val siteRepository: SiteRepository,
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val deviceRepository: DeviceRepository,
    private val patrolRouteRepository: PatrolRouteRepository,
    private val patrolRunRepository: PatrolRunRepository,
    private val invitationRepository: InvitationRepository,
    private val challengeUsedRepository: ChallengeUsedRepository,
    private val patrolRouteCheckpointRepository: PatrolRouteCheckpointRepository,
    private val checkpointRepository: CheckpointRepository,
    private val patrolScanEventRepository: PatrolScanEventRepository,
    private val checkpointSubCheckRepository: CheckpointSubCheckRepository,
    private val patrolSubCheckEventRepository: PatrolSubCheckEventRepository,
    private val attachmentRepository: ge.tiger8bit.repository.AttachmentRepository
) {

    /**
     * Очищает все таблицы БД.
     * Вызывайте в beforeEach или beforeTest для изоляции тестов.
     */
    fun cleanAll() {
        try {
            // Порядок важен из-за FK constraints
            attachmentRepository.deleteAll()
            patrolSubCheckEventRepository.deleteAll()
            patrolScanEventRepository.deleteAll()
            patrolRunRepository.deleteAll()
            patrolRouteCheckpointRepository.deleteAll()
            patrolRouteRepository.deleteAll()

            // Devices, UserRoles
            deviceRepository.deleteAll()
            userRoleRepository.deleteAll()

            // Invitations, ChallengeUsed
            invitationRepository.deleteAll()
            challengeUsedRepository.deleteAll()

            // Checkpoints
            checkpointSubCheckRepository.deleteAll()
            checkpointRepository.deleteAll()

            // Users, Sites, Organizations (last due to FK)
            userRepository.deleteAll()
            siteRepository.deleteAll()
            organizationRepository.deleteAll()
        } catch (e: Exception) {
            println("WARNING: cleanAll failed: ${e.message}")
        }
    }


    fun createOrganization(name: String = "Test Org ${UUID.randomUUID()}"): Organization {
        val org = organizationRepository.save(Organization(name = name))
        organizationRepository.flush()
        return org
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
        status: ge.tiger8bit.dto.PatrolRunStatus = ge.tiger8bit.dto.PatrolRunStatus.IN_PROGRESS
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
        email: String = TestData.Emails.unique("user"),
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
        email: String = TestData.Emails.unique("user"),
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

    fun getPatrolSubCheckEvents(scanEventId: UUID): List<PatrolSubCheckEvent> {
        return patrolSubCheckEventRepository.findByScanEventId(scanEventId)
    }

    fun getAttachments(entityType: AttachmentEntityType, entityId: UUID): List<Attachment> {
        return attachmentRepository.findByEntityTypeAndEntityId(entityType, entityId)
    }
}
