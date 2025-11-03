package ge.tiger8bit

import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import io.micronaut.gcp.function.http.HttpFunction

class GcpHttpFunction : HttpFunction() {
    override fun service(request: HttpRequest, response: HttpResponse) {
        super.service(request, response)
    }
}

