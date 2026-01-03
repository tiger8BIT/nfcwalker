package ge.tiger8bit.service

import ge.tiger8bit.domain.Role
import ge.tiger8bit.domain.User
import ge.tiger8bit.domain.UserRole
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.InvitationRepository
import ge.tiger8bit.repository.UserRepository
import ge.tiger8bit.repository.UserRoleRepository
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.*

@Singleton
open class AuthService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val invitationRepository: InvitationRepository
) {
    private val logger = getLogger()

    @Transactional
    open fun getOrCreateUserFromGoogle(googleId: String, email: String, name: String): User {
        // Try to find by googleId
        val existingByGoogleId = userRepository.findByGoogleId(googleId)
        if (existingByGoogleId.isPresent) {
            logger.info("User found by googleId: {}", googleId)
            return existingByGoogleId.get()
        }

        // Try to find by email
        val existingUser = userRepository.findByEmail(email)
        if (existingUser.isPresent) {
            val user = existingUser.get()
            // Update googleId if not set
            if (user.googleId == null) {
                user.googleId = googleId
                user.updatedAt = Instant.now()
                userRepository.update(user)
                logger.info("Updated existing user with googleId: {}", email)
            }
            return user
        }

        // Create new user
        val newUser = User(
            email = email,
            name = name,
            googleId = googleId
        )
        val savedUser = userRepository.save(newUser)
        logger.info("Created new user from Google: {}", email)
        return savedUser
    }

    @Transactional
    open fun acceptInvitation(invitationToken: String, userId: UUID): Boolean {
        val invitation = invitationRepository.findByToken(invitationToken)
        if (invitation.isEmpty) {
            logger.warn("Invitation token not found: {}", invitationToken)
            return false
        }

        val inv = invitation.get()

        // Check status
        if (inv.status != "pending") {
            logger.warn("Invitation already processed: {}", invitationToken)
            return false
        }

        // Check expiration
        if (Instant.now().isAfter(inv.expiresAt)) {
            logger.warn("Invitation expired: {}", invitationToken)
            inv.status = "expired"
            invitationRepository.update(inv)
            return false
        }

        // Create role for user
        val userRole = UserRole(
            userId = userId,
            organizationId = inv.organizationId,
            role = inv.role
        )
        userRoleRepository.save(userRole)

        // Mark invitation as accepted
        inv.status = "accepted"
        inv.acceptedAt = Instant.now()
        invitationRepository.update(inv)

        logger.info("Invitation accepted: user={}, org={}, role={}", userId, inv.organizationId, inv.role)
        return true
    }

    fun getUserRoles(userId: UUID): List<UserRole> {
        return userRoleRepository.findByIdUserId(userId)
    }

    fun getUserRoles(userId: UUID, organizationId: UUID): List<Role> {
        return userRoleRepository.findByIdUserId(userId)
            .filter { it.id.organizationId == organizationId }
            .map { it.role }
    }

    fun getUserRole(userId: UUID, organizationId: UUID): Role? {
        return userRoleRepository.findByIdUserId(userId)
            .find { it.id.organizationId == organizationId }
            ?.role
    }
}

