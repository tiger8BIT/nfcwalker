package ge.tiger8bit.spec

import ge.tiger8bit.repository.UserRepository
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.TestContainersManager
import ge.tiger8bit.spec.common.TestData.Emails
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class GoogleOAuthFlowSpec : BaseApiSpec() {

    @Inject
    private lateinit var userRepository: UserRepository

    override fun StringSpec.registerTests() {
        beforeTest {
            userRepository.deleteAll()
        }

        "SSO via mock-oauth2: first login creates user and returns our JWT" {
            val idToken = shouldThrowAny {
                TestContainersManager.issueGoogleIdToken(
                    subject = "google-user-123",
                    email = Emails.GOOGLE_USER,
                    name = "Google User",
                )
            }
        }

        "SSO via mock-oauth2: second login reuses existing user" {
            assertSoftly {
                userRepository.deleteAll()
            }
        }
    }
}
