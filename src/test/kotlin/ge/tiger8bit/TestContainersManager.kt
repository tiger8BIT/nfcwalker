package ge.tiger8bit

import com.buralotech.oss.testcontainers.mockoauth2.MockOAuth2Container
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object TestContainersManager {
    private val logger = getLogger()

    // Mailhog SMTP + HTTP UI
    val mailhog: GenericContainer<*> by lazy {
        GenericContainer(DockerImageName.parse("mailhog/mailhog:latest")).apply {
            withExposedPorts(1025, 8025)
            start()
            logger.info(
                "MailHog container started: SMTP on port {}, HTTP API on port {}",
                getMappedPort(1025), getMappedPort(8025)
            )
        }
    }

    // Mock OAuth2 server for SSO / JWKS (used via TestContainersProvider)
    val oauth2: MockOAuth2Container by lazy {
        MockOAuth2Container(DockerImageName.parse("mock-oauth2-server:latest")).apply {
            withReuse(true)
            start()
            logger.info("Mock OAuth2 container started")
        }
    }

    fun getSmtpPort(): Int = mailhog.getMappedPort(1025)
}
