package ge.tiger8bit.repository

import ge.tiger8bit.domain.Invitation
import ge.tiger8bit.dto.InvitationStatus
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface InvitationRepository : JpaRepository<Invitation, UUID> {
    fun findByToken(token: String): Optional<Invitation>
    fun findByEmailAndOrganizationId(email: String, organizationId: UUID): List<Invitation>
    fun findByOrganizationId(organizationId: UUID): List<Invitation>
    fun findByStatus(status: InvitationStatus): List<Invitation>
}

