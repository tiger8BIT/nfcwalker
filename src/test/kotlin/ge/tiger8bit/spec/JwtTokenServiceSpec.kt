package ge.tiger8bit.spec

import com.nimbusds.jwt.SignedJWT
import ge.tiger8bit.TestFixtures
import ge.tiger8bit.domain.Role
import ge.tiger8bit.service.JwtTokenService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class JwtTokenServiceSpec @Inject constructor(
    private val jwtTokenService: JwtTokenService,
    private val beanContext: io.micronaut.context.BeanContext
) : StringSpec({

    "generate JWT for user with roles" {
        val org = TestFixtures.createOrganization("JWT Org")
        val (user, _) = TestFixtures.createUserWithRole(org.id!!, Role.ROLE_BOSS)

        val token = jwtTokenService.generateForUser(requireNotNull(user.id))
        val jwt = SignedJWT.parse(token)
        val claims = jwt.jwtClaimsSet

        claims.subject shouldBe user.id.toString()
        val roles = claims.getClaim("roles") as List<*>
        roles.map { it.toString() }.shouldContain("ROLE_BOSS")
    }
})
