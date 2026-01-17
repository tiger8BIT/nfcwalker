package ge.tiger8bit.service

import ge.tiger8bit.domain.Device
import ge.tiger8bit.dto.DeviceStatus
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.DeviceRepository
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.*

@Singleton
open class DeviceService(
    private val deviceRepository: DeviceRepository
) {
    private val logger = getLogger()

    @Transactional
    open fun registerDevice(
        userId: UUID,
        organizationId: UUID,
        deviceId: String,
        metadata: String? = null
    ): Device {
        // Check if device already registered in organization
        deviceRepository.findByOrganizationIdAndDeviceId(organizationId, deviceId)
            .ifPresent {
                logger.warn("Device already registered in org: device_id={}, org={}", deviceId, organizationId)
                throw IllegalArgumentException("Device already registered")
            }

        val device = Device(
            userId = userId,
            organizationId = organizationId,
            deviceId = deviceId,
            metadata = metadata
        )
        val saved = deviceRepository.save(device)
        logger.info("Device registered: user={}, device_id={}, org={}", userId, deviceId, organizationId)
        return saved
    }

    @Transactional
    open fun updateLastUsed(deviceId: UUID) {
        val device = deviceRepository.findById(deviceId)
        if (device.isPresent) {
            val dev = device.get()
            dev.lastUsedAt = Instant.now()
            deviceRepository.update(dev)
        }
    }

    @Transactional
    open fun revokeDevice(deviceId: UUID): Boolean {
        val device = deviceRepository.findById(deviceId)
        if (device.isPresent) {
            val dev = device.get()
            dev.status = DeviceStatus.BLOCKED
            deviceRepository.update(dev)
            logger.info("Device revoked: {}", deviceId)
            return true
        }
        return false
    }

    fun getUserDevices(userId: UUID, organizationId: UUID): List<Device> {
        return deviceRepository.findByUserIdAndOrganizationId(userId, organizationId)
            .filter { it.status == DeviceStatus.ACTIVE }
    }

    fun getDeviceByIdInOrg(organizationId: UUID, deviceId: String): Device? {
        return deviceRepository.findByOrganizationIdAndDeviceId(organizationId, deviceId)
            .orElse(null)
    }

    fun getDeviceEntity(id: UUID): Device? = deviceRepository.findById(id).orElse(null)
}
