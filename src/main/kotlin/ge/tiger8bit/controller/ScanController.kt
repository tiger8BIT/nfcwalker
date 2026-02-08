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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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
        @Part("photos") photos: Publisher<CompletedFileUpload>? = null,
        @Part("subPhotos") subPhotos: Publisher<CompletedFileUpload>? = null,
        principal: Principal
    ): Mono<FinishScanResponse> {
        val userId = UUID.fromString(principal.name)

        val photosFlux = if (photos != null) Flux.from(photos) else Flux.empty()
        val subPhotosFlux = if (subPhotos != null) Flux.from(subPhotos) else Flux.empty()

        return Flux.concat(photosFlux, subPhotosFlux)
            .collectList()
            .map { allPhotos ->
                logger.info("finishScan multipart: challenge={}, totalPhotos={}", metadata.challenge, allPhotos.size)

                if (allPhotos.isEmpty()) {
                    scanService.finishScan(metadata, userId)
                } else {
                    scanService.finishScanWithPhotos(metadata, allPhotos, userId)
                }
            }
    }

    @Post("/events/{scanEventId}/photos", consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA])
    @Secured("ROLE_WORKER", "ROLE_BOSS")
    fun addPhotosToScanEvent(
        @PathVariable scanEventId: UUID,
        @Part("photos") photos: Publisher<CompletedFileUpload>?,
        principal: Principal
    ): Mono<HttpResponse<Map<String, String>>> {
        return (if (photos != null) Flux.from(photos).collectList() else Mono.just(emptyList()))
            .map { allPhotos ->
                val userId = UUID.fromString(principal.name)
                logger.info("addPhotosToScanEvent: scanEventId={}, photosCount={}", scanEventId, allPhotos.size)
                scanService.addPhotosToScanEvent(scanEventId, allPhotos, userId)
                HttpResponse.ok(mapOf("status" to "uploaded", "count" to allPhotos.size.toString()))
            }
    }

    @Delete("/events/{scanEventId}/photos/{photoId}")
    @Secured("ROLE_WORKER", "ROLE_BOSS")
    fun deletePhotoFromScanEvent(
        @PathVariable scanEventId: UUID,
        @PathVariable photoId: UUID,
        principal: Principal
    ): HttpResponse<Map<String, String>> {
        val userId = UUID.fromString(principal.name)
        logger.info("deletePhotoFromScanEvent: scanEventId={}, photoId={}", scanEventId, photoId)
        scanService.deletePhotoFromScanEvent(scanEventId, photoId, userId)
        return HttpResponse.ok(mapOf("status" to "deleted"))
    }
}
