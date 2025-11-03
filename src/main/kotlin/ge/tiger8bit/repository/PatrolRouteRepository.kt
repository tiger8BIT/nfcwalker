package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolRoute
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface PatrolRouteRepository : JpaRepository<PatrolRoute, Long>

