package shortlinkapp.api.service

import ca.jois.shortlink.generator.NaiveShortCodeGenerator
import ca.jois.shortlink.generator.ShortCodeGenerator
import ca.jois.shortlink.manager.RealShortLinkManager
import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.persistence.ShortLinkStore
import ca.jois.shortlink.util.logging.Logging
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.file.FileService
import java.nio.file.Paths
import java.time.Clock
import shortlinkapp.api.service.shortlink.ShortLinkApiService
import shortlinkapp.api.service.shortlink.ShortLinkRedirectService
import shortlinkapp.api.service.shortlink.actions.CreateShortLinkAction
import shortlinkapp.api.service.shortlink.actions.DeleteShortLinkAction
import shortlinkapp.api.service.shortlink.actions.GetShortLinkAction
import shortlinkapp.api.service.shortlink.actions.UpdateShortLinkAction

class WebServer(private val port: Int, private val shortLinkStore: ShortLinkStore) {
    private val shortLinkManager: ShortLinkManager
    private val shortCodeGenerator: ShortCodeGenerator
    private val createShortLinkAction: CreateShortLinkAction
    private val getShortLinkAction: GetShortLinkAction
    private val updateShortLinkAction: UpdateShortLinkAction
    private val deleteShortLinkAction: DeleteShortLinkAction
    private val shortLinkRedirectService: ShortLinkRedirectService
    private val shortLinkApiService: ShortLinkApiService

    init {
        with(Clock.systemUTC()) {
            shortCodeGenerator = NaiveShortCodeGenerator()
            shortLinkManager =
                RealShortLinkManager(
                    shortCodeGenerator = shortCodeGenerator,
                    shortLinkStore = shortLinkStore
                )
            createShortLinkAction = CreateShortLinkAction(shortLinkManager)
            getShortLinkAction = GetShortLinkAction(shortLinkManager)
            updateShortLinkAction = UpdateShortLinkAction(shortLinkManager)
            deleteShortLinkAction = DeleteShortLinkAction(shortLinkManager)
            shortLinkApiService =
                ShortLinkApiService(
                    createShortLinkAction,
                    getShortLinkAction,
                    updateShortLinkAction,
                    deleteShortLinkAction,
                )
            shortLinkRedirectService = ShortLinkRedirectService(shortLinkManager)
        }
    }

    fun run() {
        val server = newServer()
        server.closeOnJvmShutdown()
        server.start().join()

        logger.info("Server has been started at http://127.0.0.1:${server.activeLocalPort()}")
    }

    private fun newServer(): Server {
        val sb: ServerBuilder = Server.builder()
        return sb.http(port)
            .annotatedService("/api", shortLinkApiService)
            .annotatedService("/r", shortLinkRedirectService)
            .serviceUnder("/", FileService.of(Paths.get("shortlink-app/frontend/build")))
            .build()
    }

    companion object {
        private val logger = Logging.getLogger<WebServer>()
    }
}
