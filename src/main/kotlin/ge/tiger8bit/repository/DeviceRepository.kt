package ge.tiger8bit.repository

import ge.tiger8bit.domain.Device
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface DeviceRepository : JpaRepository<Device, UUID> {
    fun findByUserIdAndOrganizationId(userId: UUID, organizationId: UUID): List<Device>
    fun findByOrganizationIdAndDeviceId(organizationId: UUID, deviceId: String): Optional<Device>
    fun findByUserId(userId: UUID): List<Device>
}

