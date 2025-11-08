package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolRoute
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.UUID

@Repository
interface PatrolRouteRepository : JpaRepository<PatrolRoute, UUID>

