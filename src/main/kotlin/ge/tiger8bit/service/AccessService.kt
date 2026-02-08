package ge.tiger8bit.service

import ge.tiger8bit.domain.Role
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.UserRoleRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import java.util.*

/**
 * Centralized access checks tying user roles to organization scope.
 * APP_OWNER acts as global override for boss-level operations.
 */
@Singleton
open class AccessService(
    private val userRoleRepository: UserRoleRepository
) {
    private val logger = getLogger()

    // Load all roles for user once per check (can be cached later)
    private fun rolesForUser(userId: UUID): List<Pair<UUID, Role>> {
        val roles = userRoleRepository.findByIdUserId(userId).map { it.id.organizationId to it.role }
        logger.info("rolesForUser({}): found {} roles: {}", userId, roles.size, roles)
        return roles
    }

    private fun isAppOwner(userId: UUID): Boolean {
        val result = rolesForUser(userId).any { it.second == Role.ROLE_APP_OWNER }
        logger.info("isAppOwner({}): {}", userId, result)
        return result
    }

    private fun hasBossInOrg(userId: UUID, orgId: UUID): Boolean {
        val result = rolesForUser(userId).any { it.first == orgId && it.second == Role.ROLE_BOSS }
        logger.info("hasBossInOrg({}, {}): {}", userId, orgId, result)
        return result
    }

    private fun hasWorkerInOrg(userId: UUID, orgId: UUID): Boolean =
        rolesForUser(userId).any { it.first == orgId && it.second == Role.ROLE_WORKER }

    fun ensureBossOrAppOwner(userId: UUID, orgId: UUID) {
        logger.info("ensureBossOrAppOwner({}, {}) - checking permissions", userId, orgId)
        val userRoles = rolesForUser(userId)

        // APP_OWNER is a global super-admin
        if (userRoles.any { it.second == Role.ROLE_APP_OWNER }) {
            logger.info("ensureBossOrAppOwner - PASSED: user is global APP_OWNER")
            return
        }

        if (userRoles.any { it.first == orgId && it.second == Role.ROLE_BOSS }) {
            logger.info("ensureBossOrAppOwner - PASSED: user is BOSS in org {}", orgId)
            return
        }
        forbidden("Boss or AppOwner role required for organization")
    }

    fun ensureWorkerOrBoss(userId: UUID, orgId: UUID) {
        if (isAppOwner(userId)) return
        if (hasWorkerInOrg(userId, orgId)) return
        if (hasBossInOrg(userId, orgId)) return
        forbidden("Worker or Boss role required for organization")
    }

    fun ensureWorkerOrBossOrAppOwner(userId: UUID, orgId: UUID) {
        if (isAppOwner(userId)) return
        if (hasBossInOrg(userId, orgId)) return
        if (hasWorkerInOrg(userId, orgId)) return
        forbidden("Worker, Boss or AppOwner role required for organization")
    }

    fun ensureAnyRoleInOrg(userId: UUID, orgId: UUID) {
        if (isAppOwner(userId)) return
        val any = rolesForUser(userId).any { it.first == orgId }
        if (any) return
        forbidden("No role assigned in organization")
    }

    fun ensureSameUser(principalUserId: UUID, claimedUserId: String) {
        val claimed = try {
            UUID.fromString(claimedUserId)
        } catch (_: Exception) {
            forbidden("Invalid claimed user id format")
        }
        if (principalUserId != claimed) forbidden("Claimed user id mismatch")
    }

    private fun forbidden(message: String): Nothing {
        logger.error("ACCESS DENIED: {}", message, Exception("Stack trace"))
        throw HttpStatusException(HttpStatus.FORBIDDEN, message)
    }
}
