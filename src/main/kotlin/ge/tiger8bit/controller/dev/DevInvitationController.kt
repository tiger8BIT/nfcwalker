package ge.tiger8bit.controller.dev

import ge.tiger8bit.repository.InvitationRepository
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Dev-only endpoints for testing purposes.
 * These endpoints expose sensitive data and should ONLY be available in local environment.
 */
@Controller("/api/dev/invitations")
@Requires(env = ["local"])
@Secured(SecurityRule.IS_ANONYMOUS)
class DevInvitationController(
    private val invitationRepository: InvitationRepository
) {
    private val logger = LoggerFactory.getLogger(DevInvitationController::class.java)

    @Get("/{id}/token")
    fun getInvitationToken(@PathVariable id: UUID): HttpResponse<Map<String, String>> {
        logger.info("GET /api/dev/invitations/{}/token - fetching token for testing", id)

        val invitation = invitationRepository.findById(id).orElse(null)
            ?: return HttpResponse.notFound()

        return HttpResponse.ok(mapOf("token" to invitation.token))
    }
}
