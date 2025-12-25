package ge.tiger8bit.configproperties

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.validation.Validated
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Validated
@ConfigurationProperties("mail.smtp")
class MailSmtpProperties {
    @field:NotBlank
    @field:Email
    lateinit var from: String
}
