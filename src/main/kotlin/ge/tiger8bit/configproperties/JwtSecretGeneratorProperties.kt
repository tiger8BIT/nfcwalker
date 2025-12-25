package ge.tiger8bit.configproperties

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.validation.Validated
import jakarta.validation.constraints.NotBlank

@Validated
@ConfigurationProperties("micronaut.security.token.jwt.signatures.secret.generator")
class JwtSecretGeneratorProperties {
    @field:NotBlank
    lateinit var secret: String
}
