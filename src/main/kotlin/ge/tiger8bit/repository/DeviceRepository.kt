package ge.tiger8bit.repository

import ge.tiger8bit.domain.Device
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

@Repository
interface DeviceRepository : JpaRepository<Device, UUID> {
    fun findByUserIdAndOrganizationId(userId: UUID, organizationId: UUID, pageable: Pageable): Page<Device>
    fun findByOrganizationIdAndDeviceId(organizationId: UUID, deviceId: String): Optional<Device>
    fun findByUserId(userId: UUID): List<Device>
}

