package ge.tiger8bit.controller

import ge.tiger8bit.getLogger
import ge.tiger8bit.service.AuthService
import ge.tiger8bit.service.JwtTokenService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule

/**
 * Google OAuth2 callback handler.
 * Micronaut Security OAuth2 performs the authentication with Google and
 * populates [Authentication] with Google claims.
 */
@Controller("/auth/oauth/google")
class GoogleOAuthController(
    private val authService: AuthService,
    private val jwtTokenService: JwtTokenService
) {
    private val logger = getLogger()

    @Get("/callback")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    fun callback(authentication: Authentication): HttpResponse<Map<String, String>> {
        logger.info("Google OAuth callback for principal={}", authentication.name)

        val attributes = authentication.attributes
        val googleId = attributes["sub"]?.toString()
            ?: return HttpResponse.badRequest(mapOf("error" to "Missing Google subject"))
        val email = attributes["email"]?.toString()
            ?: return HttpResponse.badRequest(mapOf("error" to "Missing email from Google"))
        val name = attributes["name"]?.toString() ?: email

        val user = authService.getOrCreateUserFromGoogle(googleId, email, name)
        val token = jwtTokenService.generateForUser(requireNotNull(user.id))

        return HttpResponse.ok(
            mapOf(
                "token" to token,
                "userId" to user.id.toString(),
                "email" to user.email
            )
        )
    }
}

