package ge.tiger8bit.service

import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import java.nio.charset.StandardCharsets

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
        // Normalize separators and strip leading slash to avoid accidental absolute paths
        val normalized = templatePath.replace('\\', '/').removePrefix("/")

        // Prevent directory traversal attacks
        if (normalized.contains("..")) {
            throw IllegalArgumentException("Invalid template path: $templatePath")
        }

        val resourcePath = "$basePath/$normalized"
        val stream = this::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Template not found: $resourcePath")

        val template = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

        return variables.entries.fold(template) { acc, (key, value) ->
            acc.replace("{{${'$'}{key}}}", value)
        }
    }
}
