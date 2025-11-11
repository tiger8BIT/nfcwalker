package ge.tiger8bit.controller

import ge.tiger8bit.dto.AcceptInvitationRequest
import ge.tiger8bit.getLogger
import ge.tiger8bit.service.AuthService
import ge.tiger8bit.service.InvitationService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.security.Principal
import java.util.*

@Controller("/auth")
class AuthController(
    private val authService: AuthService,
    private val invitationService: InvitationService
) {
    private val logger = getLogger()

    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    fun getMe(principal: Principal): HttpResponse<Map<String, String>> {
        logger.info("GET /auth/me - principal: {}", principal.name)
        return HttpResponse.ok(mapOf("userId" to principal.name))
    }

    @Post("/invite/accept")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    fun acceptInvitation(
        @Body request: AcceptInvitationRequest,
        principal: Principal
    ): HttpResponse<Map<String, Any>> {
        logger.info("POST /auth/invite/accept - token: {}", request.token.take(10))

        val userId = UUID.fromString(principal.name)
        val invitation = invitationService.getValidInvitation(request.token)

        if (invitation == null) {
            logger.warn("Invalid or expired invitation token")
            return HttpResponse.badRequest(mapOf("error" to "Invalid or expired invitation"))
        }

        val accepted = authService.acceptInvitation(request.token, userId)
        val response: Map<String, Any> = if (accepted) {
            mapOf("status" to "accepted", "role" to invitation.role.name)
        } else {
            mapOf("error" to "Failed to accept invitation")
        }
        return if (accepted) {
            HttpResponse.ok(response)
        } else {
            HttpResponse.badRequest(response)
        }
    }

    @Get("/health")
    fun health(): HttpResponse<Map<String, String>> {
        return HttpResponse.ok(mapOf("status" to "ok"))
    }
}

