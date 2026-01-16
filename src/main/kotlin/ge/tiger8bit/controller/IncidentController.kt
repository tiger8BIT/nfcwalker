package ge.tiger8bit.controller

import ge.tiger8bit.dto.IncidentCreateRequest
import ge.tiger8bit.dto.IncidentPatchRequest
import ge.tiger8bit.dto.IncidentResponse
import ge.tiger8bit.dto.IncidentStatus
import ge.tiger8bit.service.IncidentService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.*

@Controller("/api/incidents")
@Secured(SecurityRule.IS_AUTHENTICATED)
class IncidentController(
    private val incidentService: IncidentService
) {

    @Post(consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA])
    @Secured("ROLE_WORKER", "ROLE_BOSS")
    fun createIncident(
        @Part("metadata") request: IncidentCreateRequest,
        @Part("photos") photos: Array<CompletedFileUpload>? = null,
        principal: Principal
    ): IncidentResponse {
        val userId = UUID.fromString(principal.name)
        return incidentService.createIncident(request, userId, photos?.toList())
    }

    @Get("/{id}")
    fun getIncident(@PathVariable id: UUID, principal: Principal): IncidentResponse {
        val userId = UUID.fromString(principal.name)
        return incidentService.getIncident(id, userId)
    }

    @Get
    fun listIncidents(
        @QueryValue organizationId: UUID,
        @QueryValue siteId: UUID? = null,
        @QueryValue status: IncidentStatus? = null,
        principal: Principal
    ): List<IncidentResponse> {
        val userId = UUID.fromString(principal.name)
        return incidentService.listIncidents(organizationId, siteId, status, userId)
    }

    @Patch("/{id}")
    @Secured("ROLE_BOSS")
    fun patchIncident(
        @PathVariable id: UUID,
        @Body request: IncidentPatchRequest,
        principal: Principal
    ): IncidentResponse {
        val userId = UUID.fromString(principal.name)
        return incidentService.patchIncident(id, request, userId)
    }

    @Post("/{id}/attach-scan/{scanEventId}")
    @Secured("ROLE_BOSS")
    fun attachToScan(
        @PathVariable id: UUID,
        @PathVariable scanEventId: UUID,
        principal: Principal
    ): IncidentResponse {
        val userId = UUID.fromString(principal.name)
        return incidentService.attachToScan(id, scanEventId, userId)
    }

    @Delete("/{id}")
    @Secured("ROLE_BOSS")
    fun deleteIncident(
        @PathVariable id: UUID,
        principal: Principal
    ) {
        val userId = UUID.fromString(principal.name)
        incidentService.deleteIncident(id, userId)
    }

    @Post("/{id}/photos", consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA])
    @Secured("ROLE_WORKER", "ROLE_BOSS")
    fun addPhotosToIncident(
        @PathVariable id: UUID,
        @Part("photos") photos: Publisher<CompletedFileUpload>?,
        principal: Principal
    ): Mono<HttpResponse<Any>> {
        return Flux.from(photos)
            .collectList()
            .map { allPhotos ->
                val userId = UUID.fromString(principal.name)
                incidentService.addPhotosToIncident(id, allPhotos, userId)
                HttpResponse.ok()
            }
    }

    @Delete("/{id}/photos/{photoId}")
    @Secured("ROLE_WORKER", "ROLE_BOSS")
    fun deletePhotoFromIncident(
        @PathVariable id: UUID,
        @PathVariable photoId: UUID,
        principal: Principal
    ) {
        val userId = UUID.fromString(principal.name)
        incidentService.deletePhotoFromIncident(id, photoId, userId)
    }
}
