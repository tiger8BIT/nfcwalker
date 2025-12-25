package ge.tiger8bit.configproperties

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.validation.Validated
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Validated
@ConfigurationProperties("app.email")
class AppEmailProperties {
    @field:NotBlank
    @field:Pattern(regexp = "^[a-z]{2}$", message = "Language must be a 2-letter code")
    lateinit var defaultLanguage: String
}
