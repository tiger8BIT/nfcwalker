package ge.tiger8bit.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import ge.tiger8bit.domain.ChallengeUsed
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

@Singleton
open class ChallengeService(
    private val em: EntityManager,
    @Property(name = "app.challenge.secret", defaultValue = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
    private val secret: String,
    @Property(name = "app.challenge.ttl-seconds", defaultValue = "60")
    private val ttlSeconds: Long
) {
    private val logger = LoggerFactory.getLogger(ChallengeService::class.java)

    fun issue(orgId: Long, deviceId: String, checkpointId: Long, ttl: Long = ttlSeconds): String {
        val now = Instant.now()
        val exp = now.plusSeconds(ttl)
        val jti = UUID.randomUUID().toString()

        val claimsSet = JWTClaimsSet.Builder()
            .jwtID(jti)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(exp))
            .claim("org", orgId)
            .claim("dev", deviceId)
            .claim("cp", checkpointId)
            .build()

        val signedJWT = SignedJWT(
            JWSHeader(JWSAlgorithm.HS256),
            claimsSet
        )

        val signer = MACSigner(secret.toByteArray())
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    open fun validateAndConsume(
        jws: String,
        expectedOrg: Long,
        expectedDev: String,
        expectedCp: Long
    ): ValidationResult {
        try {
            val signedJWT = SignedJWT.parse(jws)
            val verifier = MACVerifier(secret.toByteArray())

            if (!signedJWT.verify(verifier)) {
                return ValidationResult.Invalid("Invalid signature")
            }

            val claims = signedJWT.jwtClaimsSet
            val now = Date()

            if (claims.expirationTime.before(now)) {
                return ValidationResult.Invalid("Challenge expired")
            }

            val org = claims.getClaim("org") as? Number ?: return ValidationResult.Invalid("Missing org claim")
            val dev = claims.getClaim("dev") as? String ?: return ValidationResult.Invalid("Missing dev claim")
            val cp = claims.getClaim("cp") as? Number ?: return ValidationResult.Invalid("Missing cp claim")

            if (org.toLong() != expectedOrg) {
                return ValidationResult.Invalid("Organization mismatch")
            }
            if (dev != expectedDev) {
                return ValidationResult.Invalid("Device mismatch")
            }
            if (cp.toLong() != expectedCp) {
                return ValidationResult.Invalid("Checkpoint mismatch")
            }

            val jti = claims.getStringClaim("jti")
            val issuedAt = claims.issueTime.toInstant()
            val expiresAt = claims.expirationTime.toInstant()

            // If JTI already present, treat as replay immediately
            val existing = em.find(ChallengeUsed::class.java, jti)
            if (existing != null) {
                logger.warn("Challenge replay detected (precheck): jti=$jti")
                return ValidationResult.Replay("Challenge already used")
            }

            // Try to insert into challenge_used and flush synchronously so DB constraint
            // violations (duplicate PK) are thrown inside the try/catch and mapped to Replay.
            val challengeUsed = ChallengeUsed().apply {
                this.jti = jti
                this.issuedAt = issuedAt
                this.expiresAt = expiresAt
                this.usedAt = Instant.now()
                this.deviceId = expectedDev
                this.checkpointId = expectedCp
            }

            return try {
                em.persist(challengeUsed)
                em.flush()
                ValidationResult.Valid(jti)
            } catch (e: Exception) {
                logger.warn("Challenge replay detected on persist: jti=$jti", e)
                ValidationResult.Replay("Challenge already used")
            }

        } catch (e: Exception) {
            logger.error("Challenge validation error", e)
            return ValidationResult.Invalid("Invalid challenge format")
        }
    }
}

sealed class ValidationResult {
    data class Valid(val jti: String) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
    data class Replay(val reason: String) : ValidationResult()
}
