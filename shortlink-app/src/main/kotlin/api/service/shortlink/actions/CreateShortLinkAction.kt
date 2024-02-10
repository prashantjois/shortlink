package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL

class CreateShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: Request): ShortLink {
        return shortLinkManager.create(
            ShortLinkUser(request.username),
            URL(request.url),
            request.expiresAt
        )
    }

    data class Request(
        val username: String,
        val url: String,
        val expiresAt: Long? = null,
    )
}
