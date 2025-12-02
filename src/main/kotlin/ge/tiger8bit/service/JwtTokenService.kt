package ge.tiger8bit.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
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
    private val signingKeyBytes: ByteArray by lazy { rawSecret.toByteArray() }

    fun generateForUser(userId: UUID): String {
        val now = Date()
        val exp = Date(now.time + 3600_000L) // 1 hour

        val roles: List<String> = userRoleRepository.findByIdUserId(userId)
            .map { it.role }
            .map { it.name }

        val normalizedRoles = normalizeRoles(roles)

        val claims = JWTClaimsSet.Builder()
            .issuer("nfcwalker")
            .subject(userId.toString())
            .issueTime(now)
            .expirationTime(exp)
            .claim("roles", normalizedRoles.distinct())
            .claim("authorities", normalizedRoles.distinct())
            .claim("permissions", normalizedRoles.distinct())
            .claim("scope", roles.joinToString(" ") { it.removePrefix("ROLE_").lowercase() })
            .build()

        val signedJWT = SignedJWT(
            com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256),
            claims
        )
        signedJWT.sign(MACSigner(signingKeyBytes))
        return signedJWT.serialize()
    }

    private fun normalizeRoles(roles: List<String>): List<String> =
        roles + roles.map { it.removePrefix("ROLE_") } + roles.map { it.removePrefix("ROLE_").uppercase() }
}

