package ge.tiger8bit.service

import ge.tiger8bit.domain.Invitation
import ge.tiger8bit.getLogger
import ge.tiger8bit.configproperties.AppCoreProperties
import ge.tiger8bit.configproperties.AppEmailProperties
import ge.tiger8bit.configproperties.MailSmtpProperties
import io.micronaut.email.Email
import io.micronaut.email.EmailSender
import jakarta.inject.Singleton
import java.nio.charset.StandardCharsets

interface InvitationEmailSender {
    fun sendInvitation(invitation: Invitation, inviterName: String)
}

@Singleton
open class EmailService(
    private val emailSender: EmailSender<Any, Any>,
    private val mailSmtpProperties: MailSmtpProperties,
    private val appCoreProperties: AppCoreProperties,
    private val appEmailProperties: AppEmailProperties
) : InvitationEmailSender {

    private val logger = getLogger()

    override fun sendInvitation(invitation: Invitation, inviterName: String) {
        val invitationUrl = "${appCoreProperties.frontendUrl}/auth/invite?token=${invitation.token}"
        val language = resolveLanguage()
        val subject = getSubject(language)
        val htmlBody = loadAndRenderTemplate(language, appCoreProperties.name, inviterName, invitationUrl)

        val emailBuilder = Email.builder()
            .from(mailSmtpProperties.from)
            .to(invitation.email)
            .subject(subject)
            .body(htmlBody)

        try {
            emailSender.send(emailBuilder)
            logger.info("Invitation email sent: to={}, subject={}", invitation.email, subject)
        } catch (e: Exception) {
            logger.error("Failed to send invitation email to {}", invitation.email, e)
        }
    }

    private fun resolveLanguage(): String = appEmailProperties.defaultLanguage

    private fun getSubject(language: String): String = when (language) {
        "ru" -> "Вы приглашены в ${appCoreProperties.name}"
        else -> "You're invited to ${appCoreProperties.name}"
    }

    private fun loadAndRenderTemplate(language: String, appName: String, inviterName: String, invitationUrl: String): String {
        val templateName = "templates/email/invitation_$language.html"
        return try {
            val template = this::class.java.classLoader.getResourceAsStream(templateName)?.use { stream ->
                stream.readBytes().toString(StandardCharsets.UTF_8)
            } ?: run {
                logger.warn("Template not found: {}, using EN as fallback", templateName)
                this::class.java.classLoader.getResourceAsStream("templates/email/invitation_en.html")?.use { stream ->
                    stream.readBytes().toString(StandardCharsets.UTF_8)
                } ?: return buildFallbackHtml(appName, inviterName, invitationUrl)
            }
            renderTemplate(template, appName, inviterName, invitationUrl)
        } catch (e: Exception) {
            logger.error("Failed to load template: {}", templateName, e)
            buildFallbackHtml(appName, inviterName, invitationUrl)
        }
    }

    private fun renderTemplate(template: String, appName: String, inviterName: String, invitationUrl: String): String =
        template
            .replace("{{appName}}", appName)
            .replace("{{inviterName}}", inviterName)
            .replace("{{invitationUrl}}", invitationUrl)

    private fun buildFallbackHtml(appName: String, inviterName: String, invitationUrl: String): String =
        """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body>
            <p>$inviterName has invited you to join $appName.</p>
            <p><a href="$invitationUrl">Accept Invitation</a></p>
            </body>
            </html>
        """.trimIndent()
}
