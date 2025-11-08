package ge.tiger8bit

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.micronaut.http.MutableHttpRequest
import java.util.*

object TestAuth {
    // Use the same test secret as in application-test.yml. Prefer env JWT_SECRET
    // when present (CI can override); otherwise use the exact 64-char hex secret from application-test.yml.
    private val rawSecret: String = System.getenv("JWT_SECRET") ?: "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    private val signingKeyBytes: ByteArray by lazy { rawSecret.toByteArray() }

    fun generateToken(subject: String = "test-user", roles: List<String> = listOf("ROLE_BOSS")): String {
        val now = Date()
        val exp = Date(now.time + 3600_000L)

        // Add common JWT claims and multiple representations of roles/scopes so Micronaut
        // security mapping picks them up regardless of configuration.
        // produce multiple role representations: original, without ROLE_ prefix, uppercase without prefix
        val normalizedRoles = roles + roles.map { it.removePrefix("ROLE_") } + roles.map { it.removePrefix("ROLE_").uppercase() }
        val authorities = normalizedRoles

        val claims = JWTClaimsSet.Builder()
            .issuer("test-issuer")
            .audience("test-audience")
            .subject(subject)
            .issueTime(now)
            .expirationTime(exp)
            .claim("roles", normalizedRoles.distinct())
            .claim("authorities", authorities.distinct())
            .claim("permissions", authorities.distinct())
            .claim("scope", roles.joinToString(" ") { it.removePrefix("ROLE_").lowercase() })
            .build()

        val signedJWT = SignedJWT(com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256), claims)
        val signer = MACSigner(signingKeyBytes)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    // Convenience helpers for common test roles
    fun generateBossToken(subject: String = "boss-user") = generateToken(subject, listOf("ROLE_BOSS"))
    fun generateWorkerToken(subject: String = "worker-user") = generateToken(subject, listOf("ROLE_WORKER"))

    // Helper to decode the token's claims (useful for debugging in tests)
    fun decodeClaims(token: String) = SignedJWT.parse(token).jwtClaimsSet.toJSONObject()

    // Validate signature locally using the same secret bytes (returns true if signature OK)
    fun validateToken(token: String): Boolean {
        val jwt = SignedJWT.parse(token)
        val verifier = MACVerifier(signingKeyBytes)
        return try {
            jwt.verify(verifier)
        } catch (_: Exception) {
            false
        }
    }
}

// Extension to easily add Authorization header to requests in tests
fun <T> MutableHttpRequest<T>.withAuth(token: String): MutableHttpRequest<T> = this.header("Authorization", "Bearer $token")
