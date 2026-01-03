package ge.tiger8bit.controller

import ge.tiger8bit.dto.AcceptInvitationRequest
import ge.tiger8bit.dto.AuthMeResponse
import ge.tiger8bit.dto.UserResponse
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.UserRepository
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
import java.time.format.DateTimeFormatter
import java.util.*

@Controller("/auth")
class AuthController(
    private val authService: AuthService,
    private val invitationService: InvitationService,
    private val userRepository: UserRepository
) {
    private val logger = getLogger()
    private val formatter = DateTimeFormatter.ISO_INSTANT

    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    fun getMe(principal: Principal): HttpResponse<AuthMeResponse> {
        logger.info("GET /auth/me - principal: {}", principal.name)
        val userId = UUID.fromString(principal.name)
        val user = userRepository.findById(userId).orElse(null)
            ?: return HttpResponse.notFound()

        val roles = authService.getUserRoles(userId)
            .associate { it.id.organizationId.toString() to it.role }

        val response = AuthMeResponse(
            user = UserResponse(
                id = user.id!!,
                email = user.email,
                name = user.name,
                createdAt = formatter.format(user.createdAt)
            ),
            roles = roles
        )

        return HttpResponse.ok(response)
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

