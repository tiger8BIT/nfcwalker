package ge.tiger8bit.controller

import ge.tiger8bit.dto.FinishScanRequest
import ge.tiger8bit.dto.FinishScanResponse
import ge.tiger8bit.dto.StartScanRequest
import ge.tiger8bit.dto.StartScanResponse
import ge.tiger8bit.service.ScanService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Part
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.util.*

@Controller("/api/scan")
@Secured("ROLE_WORKER", "ROLE_BOSS")
class ScanController(
    private val scanService: ScanService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(ScanController::class.java)

    @Post("/start")
    fun startScan(@Body request: StartScanRequest, principal: Principal): StartScanResponse {
        val userId = UUID.fromString(principal.name)
        return scanService.startScan(request, userId)
    }

    @Post("/finish", consumes = [io.micronaut.http.MediaType.APPLICATION_JSON])
    fun finishScanJson(
        @Body request: FinishScanRequest,
        principal: Principal
    ): FinishScanResponse {
        val userId = UUID.fromString(principal.name)
        return scanService.finishScan(request, userId)
    }

    @Post("/finish", consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA])
    fun finishScanMultipart(
        @Part("metadata") metadata: FinishScanRequest,
        @Part("photos") photos: Array<CompletedFileUpload>? = null,
        @Part("subPhotos") subPhotos: Array<CompletedFileUpload>? = null,
        principal: Principal
    ): FinishScanResponse {
        val userId = UUID.fromString(principal.name)
        val allPhotos = (photos?.toList() ?: emptyList()) + (subPhotos?.toList() ?: emptyList())

        logger.info("finishScan multipart: challenge={}, photos={}, subPhotos={}", metadata.challenge, photos?.size, subPhotos?.size)

        return if (allPhotos.isEmpty()) {
            scanService.finishScan(metadata, userId)
        } else {
            scanService.finishScanWithPhotos(metadata, allPhotos, userId)
        }
    }
}
