package ge.tiger8bit.repository

import ge.tiger8bit.domain.PatrolRun
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface PatrolRunRepository : JpaRepository<PatrolRun, Long>

