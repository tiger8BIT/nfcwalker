package ge.tiger8bit.spec.common

import com.nimbusds.jwt.SignedJWT
import ge.tiger8bit.domain.Role
import ge.tiger8bit.service.JwtTokenService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class JwtTokenServiceSpec : BaseApiSpec() {

    @Inject
    private lateinit var jwtTokenService: JwtTokenService

    override fun StringSpec.registerTests() {
        "generate JWT for user with roles" {
            val org = fixtures.createOrganization("JWT Org")
            val (user, _) = fixtures.createUserWithRole(org.id!!, Role.ROLE_BOSS)

            val token = jwtTokenService.generateForUser(requireNotNull(user.id))
            val jwt = SignedJWT.parse(token)
            val claims = jwt.jwtClaimsSet

            claims.subject shouldBe user.id.toString()
            val rawRoles = claims.getClaim("roles")
            val roles = when (rawRoles) {
                is List<*> -> rawRoles
                is Array<*> -> rawRoles.toList()
                null -> emptyList<Any>()
                else -> listOf(rawRoles)
            }
            roles.map { it.toString() }.shouldContain("ROLE_BOSS")
        }
    }
}
