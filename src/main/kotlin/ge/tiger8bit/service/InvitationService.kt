package ge.tiger8bit.service

import ge.tiger8bit.domain.Invitation
import ge.tiger8bit.domain.Role
import ge.tiger8bit.dto.InvitationStatus
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.InvitationRepository
import ge.tiger8bit.repository.UserRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.*

@Singleton
open class InvitationService(
    private val invitationRepository: InvitationRepository,
    private val emailSender: InvitationEmailSender,
    private val userRepository: UserRepository
) {
    private val logger = getLogger()

    /**
     * Checks if inviter can invite a user with targetRole.
     * Enforces role hierarchy:
     * - APP_OWNER can invite: BOSS
     * - BOSS can invite: WORKER
     * - WORKER cannot invite anyone
     */
    private fun canInviteRole(inviterRole: Role, targetRole: Role): Boolean {
        return when (inviterRole) {
            Role.ROLE_APP_OWNER -> targetRole == Role.ROLE_BOSS
            Role.ROLE_BOSS -> targetRole == Role.ROLE_WORKER
            Role.ROLE_WORKER -> false
        }
    }

    @Transactional
    open fun createInvitation(
        email: String,
        organizationId: UUID,
        role: Role,
        createdBy: UUID,
        inviterRole: Role,
        ttlDays: Long = 7
    ): Invitation {
        // Validate role hierarchy
        if (!canInviteRole(inviterRole, role)) {
            logger.warn("Role hierarchy violation: inviter={} cannot invite role={}", inviterRole, role)
            throw HttpStatusException(
                HttpStatus.FORBIDDEN,
                "Role $inviterRole cannot invite users with role $role"
            )
        }

        val token = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plusSeconds(ttlDays * 86400)

        val invitation = Invitation(
            email = email,
            organizationId = organizationId,
            role = role,
            token = token,
            expiresAt = expiresAt,
            createdBy = createdBy
        )

        val saved = invitationRepository.save(invitation)
        logger.info(
            "Invitation created: email={}, org={}, role={}, invitedBy={}",
            email,
            organizationId,
            role,
            createdBy
        )

        val inviterName = userRepository.findById(createdBy)
            .map { it.name }
            .orElse("User")

        emailSender.sendInvitation(saved, inviterName)

        return saved
    }

    @Transactional
    open fun getValidInvitation(token: String): Invitation? {
        val invitation = invitationRepository.findByToken(token).orElse(null)
            ?: return null

        // Check status and expiration
        if (invitation.status != InvitationStatus.PENDING) {
            logger.warn("Invitation not pending: {}", token)
            return null
        }

        if (Instant.now().isAfter(invitation.expiresAt)) {
            logger.warn("Invitation expired: {}", token)
            invitation.status = InvitationStatus.EXPIRED
            invitationRepository.update(invitation)
            return null
        }

        return invitation
    }

    fun getOrganizationInvitations(organizationId: UUID): List<Invitation> =
        invitationRepository.findByOrganizationId(organizationId)

    @Transactional
    open fun cancelInvitation(invitationId: UUID): Boolean {
        val invitationOpt = invitationRepository.findById(invitationId)
        if (invitationOpt.isPresent) {
            val invitation = invitationOpt.get()
            if (invitation.status == InvitationStatus.PENDING) {
                invitation.status = InvitationStatus.REVOKED
                invitationRepository.update(invitation)
                logger.info("Invitation cancelled: {}", invitationId)
                return true
            }
        }
        return false
    }

    fun getInvitationById(id: UUID): Invitation? =
        invitationRepository.findById(id).orElse(null)
}
