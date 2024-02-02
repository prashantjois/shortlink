package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortLink
import java.net.URL

class CreateShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: Request): ShortLink {
        return shortLinkManager.create(URL(request.url), request.expiresAt)
    }

    data class Request(
        val url: String,
        val expiresAt: Long? = null,
    )
}
