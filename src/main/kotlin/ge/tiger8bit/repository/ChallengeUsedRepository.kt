package ge.tiger8bit.repository

import ge.tiger8bit.domain.ChallengeUsed
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface ChallengeUsedRepository : JpaRepository<ChallengeUsed, String>

