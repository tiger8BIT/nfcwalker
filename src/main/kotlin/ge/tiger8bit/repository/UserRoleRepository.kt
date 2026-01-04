package ge.tiger8bit.repository

import ge.tiger8bit.domain.UserRole
import ge.tiger8bit.domain.UserRoleId
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface UserRoleRepository : JpaRepository<UserRole, UserRoleId> {
    fun findByIdUserId(userId: UUID): List<UserRole>
    fun findByIdOrganizationId(organizationId: UUID): List<UserRole>
}




