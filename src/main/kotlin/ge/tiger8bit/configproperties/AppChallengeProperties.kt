package ge.tiger8bit.configproperties

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.validation.Validated
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

@Validated
@ConfigurationProperties("app.challenge")
class AppChallengeProperties {
    @field:NotBlank
    lateinit var secret: String

    @field:Positive
    var ttlSeconds: Long = 60
}
