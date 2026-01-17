package ge.tiger8bit.controller

import ge.tiger8bit.dto.DeviceResponse
import ge.tiger8bit.dto.RegisterDeviceRequest
import ge.tiger8bit.getLogger
import ge.tiger8bit.service.AccessService
import ge.tiger8bit.service.DeviceService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import java.security.Principal
import java.time.format.DateTimeFormatter
import java.util.*

@Controller("/api/devices")
class DeviceController(
    private val deviceService: DeviceService,
    private val accessService: AccessService
) {
    private val logger = getLogger()
    private val formatter = DateTimeFormatter.ISO_INSTANT

    @Post
    @Secured("ROLE_WORKER")
    fun registerDevice(
        @Body request: RegisterDeviceRequest,
        @QueryValue organizationId: UUID,
        principal: Principal
    ): HttpResponse<DeviceResponse> {
        logger.info("POST /api/devices - device_id: {}, org: {}", request.deviceId, organizationId)

        val userId = UUID.fromString(principal.name)
        accessService.ensureWorkerOrBoss(userId, organizationId)

        return try {
            val device = deviceService.registerDevice(
                userId = userId,
                organizationId = organizationId,
                deviceId = request.deviceId,
                metadata = request.metadata
            )
            HttpResponse.created(device.toResponse())
        } catch (e: IllegalArgumentException) {
            logger.warn("Failed to register device: {}", e.message)
            HttpResponse.badRequest()
        }
    }

    @Get
    @Secured("ROLE_WORKER")
    fun getUserDevices(
        @QueryValue organizationId: UUID,
        principal: Principal
    ): HttpResponse<List<DeviceResponse>> {
        logger.info("GET /api/devices - org: {}", organizationId)

        val userId = UUID.fromString(principal.name)
        accessService.ensureWorkerOrBoss(userId, organizationId)

        val devices = deviceService.getUserDevices(userId, organizationId)
        return HttpResponse.ok(devices.map { it.toResponse() })
    }

    @Delete("/{id}")
    @Secured("ROLE_WORKER")
    fun revokeDevice(id: UUID, principal: Principal): HttpResponse<Map<String, String>> {
        logger.info("DELETE /api/devices/{} - revoking", id)

        val userId = UUID.fromString(principal.name)
        val device = deviceService.getDeviceEntity(id) ?: return HttpResponse.notFound()
        accessService.ensureWorkerOrBoss(userId, device.organizationId)
        if (device.userId != userId) {
            return HttpResponse.status<Map<String, String>>(io.micronaut.http.HttpStatus.FORBIDDEN)
        }

        val success = deviceService.revokeDevice(id)
        return if (success) {
            HttpResponse.ok(mapOf("status" to "revoked"))
        } else {
            HttpResponse.notFound()
        }
    }

    private fun ge.tiger8bit.domain.Device.toResponse(): DeviceResponse {
        return DeviceResponse(
            id = this.id!!,
            deviceId = this.deviceId,
            metadata = this.metadata,
            status = this.status,
            registeredAt = this.registeredAt,
            lastUsedAt = this.lastUsedAt
        )
    }
}
