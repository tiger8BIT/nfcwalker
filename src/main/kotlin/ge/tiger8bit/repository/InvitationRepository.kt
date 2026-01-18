package ge.tiger8bit.repository

import ge.tiger8bit.domain.Invitation
import ge.tiger8bit.dto.InvitationStatus
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.util.*

@Repository
interface InvitationRepository : JpaRepository<Invitation, UUID> {
    fun findByToken(token: String): Optional<Invitation>
    fun findByEmailAndOrganizationId(email: String, organizationId: UUID): List<Invitation>
    fun findByOrganizationId(organizationId: UUID, pageable: Pageable): Page<Invitation>
    fun findByStatus(status: InvitationStatus): List<Invitation>
}

