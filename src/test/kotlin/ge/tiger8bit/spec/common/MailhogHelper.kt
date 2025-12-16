package ge.tiger8bit.spec.common

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.netty.DefaultHttpClient
import io.micronaut.json.JsonMapper
import java.net.URI

data class MailhogMessage(
    val id: String,
    val from: String,
    val to: List<String>,
    val subject: String,
    val body: String,
    val html: String?,
    val headers: Map<String, List<String>>
)

object MailhogHelper {

    private fun getApiUrl(): String {
        val mailhog = TestContainersManager.mailhog
        return "http://${mailhog.host}:${mailhog.getMappedPort(8025)}"
    }

    private fun createClient(): HttpClient = DefaultHttpClient(URI.create(getApiUrl()))

    fun getMessages(): List<MailhogMessage> {
        createClient().use { client ->
            val response = client.toBlocking().retrieve(
                HttpRequest.GET<String>("/api/v2/messages"),
                String::class.java
            )
            return parseMessages(response)
        }
    }

    fun getMessagesForRecipient(email: String): List<MailhogMessage> =
        getMessages().filter { it.to.any { to -> to.contains(email, ignoreCase = true) } }

    fun getLatestMessage(): MailhogMessage? = getMessages().firstOrNull()

    fun getLatestMessageForRecipient(email: String): MailhogMessage? =
        getMessagesForRecipient(email).firstOrNull()

    fun clearMessages() {
        createClient().use { client ->
            client.toBlocking().exchange(
                HttpRequest.DELETE<Any>("/api/v1/messages"),
                String::class.java
            )
        }
    }

    fun waitForMessage(
        recipient: String,
        timeoutMs: Long = 10000,
        pollIntervalMs: Long = 100
    ): MailhogMessage {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            getLatestMessageForRecipient(recipient)?.let { return it }
            Thread.sleep(pollIntervalMs)
        }
        error("No message received for $recipient within ${timeoutMs}ms")
    }

    fun assertMessageContains(message: MailhogMessage, vararg expectedSubstrings: String) {
        val content = "${message.subject} ${message.body} ${message.html ?: ""}"
        expectedSubstrings.forEach { expected ->
            require(content.contains(expected, ignoreCase = true)) {
                "Expected message to contain '$expected' but it didn't.\nSubject: ${message.subject}\nBody: ${message.body}"
            }
        }
    }

    fun assertMessageSentTo(message: MailhogMessage, expectedRecipient: String) {
        require(message.to.any { it.contains(expectedRecipient, ignoreCase = true) }) {
            "Expected message to be sent to '$expectedRecipient' but was sent to: ${message.to}"
        }
    }

    fun assertSubjectContains(message: MailhogMessage, expected: String) {
        require(message.subject.contains(expected, ignoreCase = true)) {
            "Expected subject to contain '$expected' but was: ${message.subject}"
        }
    }

    private fun parseMessages(json: String): List<MailhogMessage> {
        val mapper = JsonMapper.createDefault()
        val root = mapper.readValue(json, Map::class.java) as Map<*, *>
        val items = root["items"] as? List<*> ?: return emptyList()

        return items.mapNotNull { item ->
            val msg = item as? Map<*, *> ?: return@mapNotNull null
            val content = msg["Content"] as? Map<*, *> ?: return@mapNotNull null
            val headers = content["Headers"] as? Map<*, *> ?: emptyMap<String, Any>()
            val raw = msg["Raw"] as? Map<*, *>

            MailhogMessage(
                id = msg["ID"]?.toString() ?: "",
                from = extractEmail(headers["From"]),
                to = extractEmails(headers["To"]),
                subject = (headers["Subject"] as? List<*>)?.firstOrNull()?.toString() ?: "",
                body = raw?.get("Data")?.toString() ?: content["Body"]?.toString() ?: "",
                html = extractHtmlBody(content),
                headers = headers.entries.associate { (k, v) ->
                    k.toString() to (v as? List<*>)?.map { it.toString() }.orEmpty()
                }
            )
        }
    }

    private fun extractEmail(value: Any?): String {
        val list = value as? List<*> ?: return ""
        return list.firstOrNull()?.toString() ?: ""
    }

    private fun extractEmails(value: Any?): List<String> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { it?.toString() }
    }

    private fun extractHtmlBody(content: Map<*, *>): String? {
        val mime = content["MIME"] as? Map<*, *> ?: return null
        val parts = mime["Parts"] as? List<*> ?: return null
        return parts.mapNotNull { part ->
            val p = part as? Map<*, *> ?: return@mapNotNull null
            val headers = p["Headers"] as? Map<*, *> ?: return@mapNotNull null
            val contentType = (headers["Content-Type"] as? List<*>)?.firstOrNull()?.toString() ?: ""
            if (contentType.contains("text/html")) {
                p["Body"]?.toString()
            } else null
        }.firstOrNull()
    }
}

