package ge.tiger8bit.service

import ge.tiger8bit.domain.Invitation
import ge.tiger8bit.domain.Role
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.InvitationRepository
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.*

@Singleton
open class InvitationService(
    private val invitationRepository: InvitationRepository
) {
    private val logger = getLogger()

    @Transactional
    open fun createInvitation(
        email: String,
        organizationId: UUID,
        role: Role,
        createdBy: UUID,
        ttlDays: Long = 7
    ): Invitation {
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
        logger.info("Invitation created: email={}, org={}, role={}, token={}", email, organizationId, role, token)
        return saved
    }

    @Transactional
    open fun getValidInvitation(token: String): Invitation? {
        val invitation = invitationRepository.findByToken(token).orElse(null)
            ?: return null

        // Check status and expiration
        if (invitation.status != "pending") {
            logger.warn("Invitation not pending: {}", token)
            return null
        }

        if (Instant.now().isAfter(invitation.expiresAt)) {
            logger.warn("Invitation expired: {}", token)
            invitation.status = "expired"
            invitationRepository.update(invitation)
            return null
        }

        return invitation
    }

    fun getOrganizationInvitations(organizationId: UUID): List<Invitation> {
        return invitationRepository.findByOrganizationId(organizationId)
    }

    @Transactional
    open fun cancelInvitation(invitationId: UUID): Boolean {
        val invitation = invitationRepository.findById(invitationId)
        if (invitation.isPresent) {
            val inv = invitation.get()
            if (inv.status == "pending") {
                inv.status = "cancelled"
                invitationRepository.update(inv)
                logger.info("Invitation cancelled: {}", invitationId)
                return true
            }
        }
        return false
    }

    fun getInvitationById(id: UUID): Invitation? =
        invitationRepository.findById(id).orElse(null)
}
