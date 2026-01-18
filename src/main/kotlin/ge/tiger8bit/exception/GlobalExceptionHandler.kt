package ge.tiger8bit.exception

import ge.tiger8bit.getLogger
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

@Singleton
@Produces
@Requires(classes = [Throwable::class, ExceptionHandler::class])
class GlobalExceptionHandler : ExceptionHandler<Throwable, HttpResponse<*>> {

    private val logger = getLogger()

    override fun handle(request: HttpRequest<*>, exception: Throwable): HttpResponse<*> {
        logger.error("Unexpected error occurred while processing request: [${request.method}] ${request.uri}", exception)

        return when (exception) {
            is IllegalArgumentException -> HttpResponse.badRequest<Map<String, String>>()
                .body(mapOf("message" to (exception.message ?: "Invalid request")))

            else -> HttpResponse.serverError<Map<String, String>>()
                .body(mapOf("message" to (exception.message ?: "Internal Server Error")))
        }
    }
}
