package shortlinkapp.api.service

import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import generator.NaiveShortCodeGenerator
import generator.ShortCodeGenerator
import java.time.Clock
import manager.RealShortLinkManager
import manager.ShortLinkManager
import persistence.InMemoryShortLinkStore
import persistence.ShortLinkStore
import shortlinkapp.api.service.shortlink.ShortLinkService
import shortlinkapp.api.service.shortlink.actions.CreateShortLinkAction
import shortlinkapp.api.service.shortlink.actions.DeleteShortLinkAction
import shortlinkapp.api.service.shortlink.actions.GetShortLinkAction
import shortlinkapp.api.service.shortlink.actions.UpdateShortLinkAction
import util.logging.Logging

class WebServer(private val port: Int) {
    private val shortLinkStore: ShortLinkStore
    private val shortLinkManager: ShortLinkManager
    private val shortCodeGenerator: ShortCodeGenerator
    private val createShortLinkAction: CreateShortLinkAction
    private val getShortLinkAction: GetShortLinkAction
    private val updateShortLinkAction: UpdateShortLinkAction
    private val deleteShortLinkAction: DeleteShortLinkAction
    private val shortLinkService: ShortLinkService

    init {
        with(Clock.systemUTC()) {
            shortCodeGenerator = NaiveShortCodeGenerator()
            shortLinkStore = InMemoryShortLinkStore()
            shortLinkManager =
                RealShortLinkManager(
                    shortCodeGenerator = shortCodeGenerator,
                    shortLinkStore = shortLinkStore
                )
            createShortLinkAction = CreateShortLinkAction(shortLinkManager)
            getShortLinkAction = GetShortLinkAction(shortLinkManager)
            updateShortLinkAction = UpdateShortLinkAction(shortLinkManager)
            deleteShortLinkAction = DeleteShortLinkAction(shortLinkManager)
            shortLinkService =
                ShortLinkService(
                    createShortLinkAction,
                    getShortLinkAction,
                    updateShortLinkAction,
                    deleteShortLinkAction,
                )
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
        return sb.http(port).annotatedService("/shortlinks", shortLinkService).build()
    }

    companion object {
        private val logger = Logging.getLogger<WebServer>()
    }
}
