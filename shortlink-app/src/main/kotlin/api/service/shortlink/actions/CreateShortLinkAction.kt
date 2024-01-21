package shortlinkapp.api.service.shortlink.actions

import java.net.URL
import manager.ShortLinkManager
import model.ShortLink

class CreateShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: Request): ShortLink {
        return shortLinkManager.create(URL(request.url), request.expiresAt)
    }

    data class Request(
        val url: String,
        val expiresAt: Long? = null,
    )
}
