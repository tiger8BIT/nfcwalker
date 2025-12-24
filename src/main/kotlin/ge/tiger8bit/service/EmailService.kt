package ge.tiger8bit.service

import ge.tiger8bit.domain.Invitation
import ge.tiger8bit.getLogger
import io.micronaut.context.annotation.Value
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
    @Value("\${mail.smtp.from}")
    private val fromEmail: String,
    @Value("\${app.name}")
    private val appName: String,
    @Value("\${app.frontend-url}")
    private val frontendUrl: String,
    @Value("\${app.email.default-language}")
    private val defaultLanguage: String
) : InvitationEmailSender {

    private val logger = getLogger()

    override fun sendInvitation(invitation: Invitation, inviterName: String) {
        val invitationUrl = "$frontendUrl/auth/invite?token=${invitation.token}"
        val language = resolveLanguage()
        val subject = getSubject(language)
        val htmlBody = loadAndRenderTemplate(language, appName, inviterName, invitationUrl)

        val emailBuilder = Email.builder()
            .from(fromEmail)
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

    private fun resolveLanguage(): String = defaultLanguage

    private fun getSubject(language: String): String = when (language) {
        "ru" -> "Вы приглашены в $appName"
        else -> "You're invited to $appName"
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
