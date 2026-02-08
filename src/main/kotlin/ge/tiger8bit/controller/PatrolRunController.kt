package ge.tiger8bit.controller

import ge.tiger8bit.dto.CreatePatrolRunRequest
import ge.tiger8bit.dto.PatrolRunResponse
import ge.tiger8bit.service.PatrolRunService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.util.*

@Controller("/api/patrol-runs")
@Secured("ROLE_BOSS", "ROLE_APP_OWNER")
class PatrolRunController(
    private val patrolRunService: PatrolRunService
) {
    @Post
    fun createRun(@Body request: CreatePatrolRunRequest, principal: Principal): PatrolRunResponse {
        val userId = UUID.fromString(principal.name)
        return patrolRunService.createRun(request, userId)
    }
}
