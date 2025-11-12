package ge.tiger8bit.spec

import ge.tiger8bit.TestFixtures
import io.kotest.core.spec.style.StringSpec
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(transactional = false)
abstract class BaseApiSpec(
    @Inject @Client("/") val client: HttpClient,
    @Inject val beanContext: io.micronaut.context.BeanContext
) : StringSpec(
    {
        TestFixtures.init(beanContext)
        @Suppress("LeakingThis")
        (this as BaseApiSpec).registerTests()
    }
) {
    protected abstract fun StringSpec.registerTests()

    /**
     * Helper-метод для POST запросов с JSON телом
     */
    @Suppress("unused")
    protected fun <T> postJson(
        url: String,
        body: Any,
        token: String? = null,
        responseType: Class<T>
    ): T {
        val request = HttpRequest.POST(url, body)
        if (token != null) {
            request.bearerAuth(token)
        }
        return client.toBlocking().retrieve(request, responseType)
    }

    /**
     * Helper-метод для GET запросов
     */
    @Suppress("unused")
    protected fun <T> getJson(
        url: String,
        token: String? = null,
        responseType: Class<T>
    ): T {
        val request = HttpRequest.GET<T>(url)
        if (token != null) {
            request.bearerAuth(token)
        }
        return client.toBlocking().retrieve(request, responseType)
    }

    /**
     * Helper-метод для GET запросов с типом Argument (для списков)
     */
    @Suppress("unused")
    protected fun <T> getJsonList(
        url: String,
        token: String? = null,
        listType: Argument<T>
    ): T {
        val request = HttpRequest.GET<T>(url)
        if (token != null) {
            request.bearerAuth(token)
        }
        return client.toBlocking().retrieve(request, listType)
    }

    /**
     * Helper-метод для DELETE запросов
     */
    @Suppress("unused")
    protected fun deleteJson(
        url: String,
        token: String? = null
    ) {
        // Use Any for request body generic and pass explicit Unit response type to avoid inference issues
        val request = HttpRequest.DELETE<Any>(url)
        if (token != null) {
            request.bearerAuth(token)
        }
        client.toBlocking().exchange(request, Unit::class.java)
    }
}
