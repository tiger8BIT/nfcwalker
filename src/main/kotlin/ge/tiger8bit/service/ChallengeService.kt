package ge.tiger8bit.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import ge.tiger8bit.constants.ChallengeConstants
import ge.tiger8bit.domain.ChallengeUsed
import ge.tiger8bit.getLogger
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.time.Instant
import java.util.*

@Singleton
open class ChallengeService(
    private val entityManager: EntityManager,
    @Property(name = "app.challenge.secret")
    private val secret: String,
    @Property(name = "app.challenge.ttl-seconds")
    private val ttlSeconds: Long
) {
    private val logger = getLogger()

    fun issue(organizationId: UUID, deviceId: String, checkpointId: UUID, ttl: Long = ttlSeconds): String {
        val now = Instant.now()
        val exp = now.plusSeconds(ttl)
        val jti = UUID.randomUUID().toString()

        val claimsSet = JWTClaimsSet.Builder()
            .jwtID(jti)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(exp))
            .claim(ChallengeConstants.Claims.ORGANIZATION, organizationId.toString())
            .claim(ChallengeConstants.Claims.DEVICE, deviceId)
            .claim(ChallengeConstants.Claims.CHECKPOINT, checkpointId.toString())
            .build()

        val signedJWT = SignedJWT(
            JWSHeader(JWSAlgorithm.HS256),
            claimsSet
        )

        val signer = MACSigner(secret.toByteArray())
        signedJWT.sign(signer)

        logger.debug("Challenge issued: jti={}, org={}, cp={}, ttl={}", jti, organizationId, checkpointId, ttl)

        return signedJWT.serialize()
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    open fun validateAndConsume(
        jws: String,
        expectedOrganizationId: UUID,
        expectedDeviceId: String,
        expectedCheckpointId: UUID
    ): ValidationResult {
        try {
            val signedJWT = SignedJWT.parse(jws)
            val verifier = MACVerifier(secret.toByteArray())

            if (!signedJWT.verify(verifier)) {
                logger.warn("Challenge signature invalid")
                return ValidationResult.Invalid("Invalid signature")
            }

            val claims = signedJWT.jwtClaimsSet
            val now = Date()

            if (claims.expirationTime.before(now)) {
                logger.warn("Challenge expired: jti={}", claims.getStringClaim("jti"))
                return ValidationResult.Invalid("Challenge expired")
            }

            val organizationId = try {
                UUID.fromString(claims.getClaim("organization") as String)
            } catch (_: Exception) {
                logger.warn("Invalid organization claim in challenge")
                return ValidationResult.Invalid("Invalid organization claim")
            }

            val deviceId = claims.getClaim("device") as? String ?: return ValidationResult.Invalid("Missing device claim")

            val checkpointId = try {
                UUID.fromString(claims.getClaim("checkpoint") as String)
            } catch (_: Exception) {
                logger.warn("Invalid checkpoint claim in challenge")
                return ValidationResult.Invalid("Invalid checkpoint claim")
            }

            if (organizationId != expectedOrganizationId) {
                logger.warn("Challenge organization mismatch: expected={}, got={}", expectedOrganizationId, organizationId)
                return ValidationResult.Invalid("Organization mismatch")
            }
            if (deviceId != expectedDeviceId) {
                logger.warn("Challenge device mismatch: expected={}, got={}", expectedDeviceId, deviceId)
                return ValidationResult.Invalid("Device mismatch")
            }
            if (checkpointId != expectedCheckpointId) {
                logger.warn("Challenge checkpoint mismatch: expected={}, got={}", expectedCheckpointId, checkpointId)
                return ValidationResult.Invalid("Checkpoint mismatch")
            }

            val jti = claims.getStringClaim("jti")
            val issuedAt = claims.issueTime.toInstant()
            val expiresAt = claims.expirationTime.toInstant()

            val existing = entityManager.find(ChallengeUsed::class.java, jti)
            if (existing != null) {
                logger.warn("Challenge replay detected (precheck): jti={}", jti)
                return ValidationResult.Replay("Challenge already used")
            }

            val challengeUsed = ChallengeUsed().apply {
                this.jti = jti
                this.issuedAt = issuedAt
                this.expiresAt = expiresAt
                this.usedAt = Instant.now()
                this.deviceId = expectedDeviceId
                this.checkpointId = expectedCheckpointId
            }

            return try {
                entityManager.persist(challengeUsed)
                entityManager.flush()
                logger.info("Challenge consumed: jti={}, org={}, cp={}", jti, expectedOrganizationId, expectedCheckpointId)
                ValidationResult.Valid(jti)
            } catch (_: Exception) {
                logger.warn("Challenge replay detected on persist: jti={}", jti)
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
