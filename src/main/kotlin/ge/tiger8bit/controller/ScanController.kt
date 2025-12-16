package ge.tiger8bit.controller

import ge.tiger8bit.dto.*
import ge.tiger8bit.service.ScanService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.util.*

@Controller("/api/scan")
@Secured("ROLE_WORKER", "ROLE_BOSS")
class ScanController(
    private val scanService: ScanService
) {
    @Post("/start")
    fun startScan(@Body request: StartScanRequest, principal: Principal): StartScanResponse {
        val userId = UUID.fromString(principal.name)
        return scanService.startScan(request, userId)
    }

    @Post("/finish")
    fun finishScan(@Body request: FinishScanRequest, principal: Principal): HttpResponse<FinishScanResponse> {
        val userId = UUID.fromString(principal.name)
        val response = scanService.finishScan(request, userId)
        return HttpResponse.ok(response)
    }
}
