package ge.tiger8bit.service

import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

/**
 * Very small, simple HTML template renderer based on {{placeholders}}.
 * Templates live under src/main/resources/templates.
 */
@Singleton
class TemplateRenderer(
    @Value("\${app.template-base:templates}")
    private val basePath: String
) {
    fun render(templatePath: String, variables: Map<String, String>): String {
        val resourcePath = "$basePath/$templatePath"
        val stream = this::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Template not found: $resourcePath")

        val template = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }

        return variables.entries.fold(template) { acc, (key, value) ->
            acc.replace("{{" + key + "}}", value)
        }
    }
}

