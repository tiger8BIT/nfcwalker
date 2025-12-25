package ge.tiger8bit.configproperties

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.validation.Validated
import jakarta.validation.constraints.NotBlank

@Validated
@ConfigurationProperties("app")
class AppCoreProperties {
    @field:NotBlank
    lateinit var name: String

    @field:NotBlank
    lateinit var frontendUrl: String
}
