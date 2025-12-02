package ge.tiger8bit

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object TestContainersManager {
    private val logger = getLogger()

    val mailhog: GenericContainer<*>? by lazy {
        try {
            logger.info("Attempting to start MailHog container for SMTP testing")
            GenericContainer(DockerImageName.parse("mailhog/mailhog:latest")).apply {
                withExposedPorts(1025, 8025)
                start()
                logger.info(
                    "MailHog container started: SMTP on port {}, HTTP API on port {}",
                    getMappedPort(1025), getMappedPort(8025)
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to start MailHog container (Docker unavailable?): {}", e.message)
            null
        }
    }

    fun getSmtpPort(): Int? = mailhog?.getMappedPort(1025)
    fun getMailhogHttpPort(): Int? = mailhog?.getMappedPort(8025)

    fun isAvailable(): Boolean = mailhog != null

    fun logStatus() {
        if (isAvailable()) {
            logger.info("Test containers: MailHog AVAILABLE")
        } else {
            logger.info("Test containers: Docker NOT AVAILABLE, using email stubs")
        }
    }
}

