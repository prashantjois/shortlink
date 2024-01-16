package shortlinkapp

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.ServiceRequestContext

class App {
    fun newServer(port: Int): Server {
        val sb: ServerBuilder = Server.builder()
        return sb.http(port)
            .service("/") { ctx: ServiceRequestContext?, req: HttpRequest? ->
                HttpResponse.of(greeting)
            }
            .build()
    }

    private val greeting: String
        get() {
            return "Hello World!"
        }
}

fun main() {
    val app = App()
    val server: Server = app.newServer(8080)

    server.closeOnJvmShutdown()

    server.start().join()

    println(
        "Server has been started. Serving dummy service at http://127.0.0.1:${server.activeLocalPort()}"
    )
}
