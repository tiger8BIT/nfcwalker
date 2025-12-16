package ge.tiger8bit.controller

import ge.tiger8bit.domain.Invitation
import ge.tiger8bit.domain.Role
import ge.tiger8bit.dto.CreateInvitationRequest
import ge.tiger8bit.dto.InvitationResponse
import ge.tiger8bit.getLogger
import ge.tiger8bit.service.AccessService
import ge.tiger8bit.service.AuthService
import ge.tiger8bit.service.InvitationService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.time.format.DateTimeFormatter
import java.util.*

@Controller("/api/invitations")
class InvitationController(
    private val invitationService: InvitationService,
    private val authService: AuthService,
    private val accessService: AccessService
) {
    private val logger = getLogger()
    private val formatter = DateTimeFormatter.ISO_INSTANT

    @Post
    @Secured("ROLE_BOSS", "ROLE_APP_OWNER")
    fun createInvitation(
        @Body request: CreateInvitationRequest,
        principal: Principal
    ): HttpResponse<InvitationResponse> {
        logger.info(
            "POST /api/invitations - email: {}, org: {}, role: {}",
            request.email,
            request.organizationId,
            request.role
        )

        val userId = UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, request.organizationId)

        val orgRole = authService.getUserRole(userId, request.organizationId)
        val inviterRole = orgRole ?: Role.ROLE_APP_OWNER

        // Дополнительная страховка: только APP_OWNER может приглашать BOSS, только BOSS - WORKER
        if (inviterRole == Role.ROLE_WORKER) {
            logger.warn("WORKER cannot create invitations: user={}, org={}", userId, request.organizationId)
            return HttpResponse.status(HttpStatus.FORBIDDEN)
        }

        val invitation = invitationService.createInvitation(
            email = request.email,
            organizationId = request.organizationId,
            role = request.role,
            createdBy = userId,
            inviterRole = inviterRole
        )

        return HttpResponse.created(invitation.toResponse())
    }

    @Get
    @Secured("ROLE_BOSS", "ROLE_APP_OWNER")
    fun getInvitations(
        @QueryValue organizationId: UUID,
        principal: Principal
    ): HttpResponse<List<InvitationResponse>> {
        logger.info("GET /api/invitations - org: {}", organizationId)

        val userId = UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, organizationId)

        val invitations = invitationService.getOrganizationInvitations(organizationId)
        return HttpResponse.ok(invitations.map { it.toResponse() })
    }

    @Delete("/{id}")
    @Secured("ROLE_BOSS", "ROLE_APP_OWNER")
    fun cancelInvitation(id: UUID, principal: Principal): HttpResponse<Map<String, String>> {
        logger.info("DELETE /api/invitations/{} - cancelling", id)

        val invitation = invitationService.getInvitationById(id) ?: return HttpResponse.notFound()
        val userId = UUID.fromString(principal.name)
        accessService.ensureBossOrAppOwner(userId, invitation.organizationId)

        val success = invitationService.cancelInvitation(id)
        return if (success) {
            HttpResponse.ok(mapOf("status" to "cancelled"))
        } else {
            HttpResponse.notFound()
        }
    }

    private fun Invitation.toResponse(): InvitationResponse =
        InvitationResponse(
            id = requireNotNull(id),
            email = email,
            organizationId = organizationId,
            role = role,
            status = status,
            expiresAt = formatter.format(expiresAt)
        )
}
