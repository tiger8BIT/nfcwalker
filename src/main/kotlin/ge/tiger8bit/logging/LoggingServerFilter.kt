package ge.tiger8bit.logging

import io.micronaut.core.order.Ordered
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.annotation.Filter
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

@Filter("/**")
@Singleton
class LoggingServerFilter : HttpServerFilter {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    private val log = LoggerFactory.getLogger(LoggingServerFilter::class.java)

    override fun doFilter(
        request: HttpRequest<*>,
        chain: ServerFilterChain
    ): Publisher<MutableHttpResponse<*>> {
        val method = request.methodName
        val uri = request.uri
        val body = request.getBody(String::class.java).orElse(null)

        log.info("IN  -> {} {} body={}", method, uri, body)

        return Flux.from(chain.proceed(request))
            .doOnNext { response ->
                log.info("OUT <- {} {} status={}", method, uri, response.status())
            }
    }
}
