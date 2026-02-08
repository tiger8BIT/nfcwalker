package ge.tiger8bit.controller.dev

import ge.tiger8bit.domain.Role
import ge.tiger8bit.domain.User
import ge.tiger8bit.domain.UserRole
import ge.tiger8bit.repository.OrganizationRepository
import ge.tiger8bit.repository.UserRepository
import ge.tiger8bit.repository.UserRoleRepository
import ge.tiger8bit.service.JwtTokenService
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

@Controller("/auth/dev")
@Requires(env = ["local"])
@Secured(SecurityRule.IS_ANONYMOUS)
open class DevAuthController(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val organizationRepository: OrganizationRepository,
    private val entityManager: EntityManager,
    private val jwtTokenService: JwtTokenService
) {
    private val logger = LoggerFactory.getLogger(DevAuthController::class.java)

    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Transactional
    open fun magicLogin(@QueryValue(defaultValue = "admin@nfcwalker.com") email: String): HttpResponse<Map<String, String>> {
        logger.info("Dev magic login for email: {}", email)

        val user = userRepository.findByEmail(email).orElseGet {
            userRepository.save(
                User(
                    email = email,
                    name = "User ${email.split("@")[0]}",
                    googleId = "dev-google-id-${email}-${System.currentTimeMillis()}"
                )
            )
        }

        // Ensure "System Root" organization exists to hold ROLE_APP_OWNER
        // This is needed because user_roles table requires an organization_id FK
        val systemOrgId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (!organizationRepository.existsById(systemOrgId)) {
            logger.info("Creating System Root organization for ROLE_APP_OWNER")
            // Use raw SQL INSERT to bypass @GeneratedValue constraint
            entityManager.createNativeQuery(
                "INSERT INTO organizations (id, name, created_at) VALUES (:id, :name, :createdAt)"
            ).apply {
                setParameter("id", systemOrgId)
                setParameter("name", "System Root")
                setParameter("createdAt", Instant.now())
            }.executeUpdate()
        }

        val hasRole = userRoleRepository.findByIdUserId(user.id!!)
            .any { it.role == Role.ROLE_APP_OWNER }

        if (!hasRole) {
            logger.info("Granting ROLE_APP_OWNER to: {}", email)
            userRoleRepository.save(UserRole(user.id!!, systemOrgId, Role.ROLE_APP_OWNER))
            userRoleRepository.flush()
        }

        val allRoles = userRoleRepository.findByIdUserId(user.id!!)
            .map { it.role.name }
            .toMutableList()

        if (!allRoles.contains(Role.ROLE_APP_OWNER.name)) allRoles.add(Role.ROLE_APP_OWNER.name)

        val token = jwtTokenService.generateForUser(user.id!!, allRoles)
        return HttpResponse.ok(
            mapOf(
                "token" to token,
                "userId" to user.id.toString(),
                "email" to user.email
            )
        )
    }
}
