package ge.tiger8bit.controller

import ge.tiger8bit.dto.FinishScanRequest
import ge.tiger8bit.dto.FinishScanResponse
import ge.tiger8bit.dto.StartScanRequest
import ge.tiger8bit.dto.StartScanResponse
import ge.tiger8bit.service.ScanService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.*

@Controller("/api/scan")
@Secured("ROLE_WORKER", "ROLE_BOSS")
class ScanController(
    private val scanService: ScanService
) {
    private val logger = LoggerFactory.getLogger(ScanController::class.java)

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
        @Part("photos") photos: Publisher<CompletedFileUpload>? = null,
        principal: Principal
    ): Mono<FinishScanResponse> {

        val userId = UUID.fromString(principal.name)

        return (if (photos != null) Flux.from(photos).collectList() else Mono.just(emptyList()))
            .map { allPhotos ->
                if (allPhotos.isEmpty()) {
                    scanService.finishScan(metadata, userId)
                } else {
                    scanService.finishScanWithPhotos(metadata, allPhotos, userId)
                }
            }
    }

    @Post("/events/{eventId}/photos", consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA])
    @Secured("ROLE_WORKER", "ROLE_BOSS")
    fun addPhotosToScanEvent(
        @PathVariable eventId: UUID,
        @Part("photos") photos: Publisher<CompletedFileUpload>? = null,
        principal: Principal
    ): Mono<HttpResponse<Any>> {
        return (if (photos != null) Flux.from(photos).collectList() else Mono.just(emptyList()))
            .map { allPhotos ->
                val userId = UUID.fromString(principal.name)
                scanService.addPhotosToScanEvent(eventId, allPhotos, userId)
                HttpResponse.ok()
            }
    }

    @Delete("/events/{eventId}/photos/{photoId}")
    @Secured("ROLE_WORKER", "ROLE_BOSS")
    fun deletePhotoFromScanEvent(
        @PathVariable eventId: UUID,
        @PathVariable photoId: UUID,
        principal: Principal
    ) {
        val userId = UUID.fromString(principal.name)
        scanService.deletePhotoFromScanEvent(eventId, photoId, userId)
    }
}
