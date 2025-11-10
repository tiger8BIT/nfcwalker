package ge.tiger8bit.logging

import io.micronaut.core.order.Ordered
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.filter.ClientFilterChain
import io.micronaut.http.filter.HttpClientFilter
import io.micronaut.http.annotation.Filter
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

@Filter("/**")
@Singleton
class LoggingClientFilter : HttpClientFilter {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    private val log = LoggerFactory.getLogger(LoggingClientFilter::class.java)

    override fun doFilter(
        request: MutableHttpRequest<*>,
        chain: ClientFilterChain
    ): Publisher<out HttpResponse<*>> {
        val method = request.methodName
        val uri = request.uri
        val body = request.getBody(String::class.java).orElse(null)

        log.info("OUT -> {} {} body={}", method, uri, body)

        return Flux.from(chain.proceed(request))
            .doOnNext { response ->
                log.info("IN  <- {} {} status={}", method, uri, response.status())
            }
    }
}
