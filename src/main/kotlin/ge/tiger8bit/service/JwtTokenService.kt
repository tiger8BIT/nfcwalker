package ge.tiger8bit.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import ge.tiger8bit.getLogger
import ge.tiger8bit.repository.UserRoleRepository
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import java.util.*

@Singleton
class JwtTokenService(
    private val userRoleRepository: UserRoleRepository,
    @Value("\${micronaut.security.token.jwt.signatures.secret.generator.secret}")
    private val rawSecret: String
) {
    private val logger = getLogger()
    private val signingKeyBytes: ByteArray by lazy { rawSecret.toByteArray() }

    fun generateForUser(userId: UUID): String {
        val now = Date()
        val exp = Date(now.time + 3600_000L) // 1 hour

        logger.info("generateForUser: fetching roles for userId={}", userId)
        val userRoles = userRoleRepository.findByIdUserId(userId)
        logger.info("generateForUser: found {} user roles", userRoles.size)
        userRoles.forEach { ur ->
            logger.info("  - role: userId={}, orgId={}, role={}", ur.id.userId, ur.id.organizationId, ur.role)
        }

        val dbRoles: List<String> = userRoles.map { it.role.name }
        logger.info("generateForUser: dbRoles={}", dbRoles)

        // Always use original role names for claim (e.g., ROLE_BOSS)
        val rolesClaim = dbRoles.distinct()
        logger.info("generateForUser: rolesClaim={}", rolesClaim)

        val claims = JWTClaimsSet.Builder()
            .issuer("nfcwalker")
            .subject(userId.toString())
            .issueTime(now)
            .expirationTime(exp)
            .claim("roles", rolesClaim)
            .claim("authorities", rolesClaim)
            .claim("permissions", rolesClaim)
            .claim("scope", dbRoles.joinToString(" ") { it.removePrefix("ROLE_").lowercase() })
            .build()

        val signedJWT = SignedJWT(
            com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256),
            claims
        )
        signedJWT.sign(MACSigner(signingKeyBytes))
        return signedJWT.serialize()
    }
}
