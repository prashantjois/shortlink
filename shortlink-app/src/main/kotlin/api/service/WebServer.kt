package shortlinkapp.api.service

import ca.jois.shortlink.generator.NaiveShortCodeGenerator
import ca.jois.shortlink.generator.ShortCodeGenerator
import ca.jois.shortlink.manager.RealShortLinkManager
import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.persistence.ShortLinkStore
import ca.jois.shortlink.util.logging.Logging
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServerBuilder
import java.time.Clock
import shortlinkapp.api.service.shortlink.ShortLinkService
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
    private val shortLinkService: ShortLinkService

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
