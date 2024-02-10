package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL

class UpdateShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: UrlRequest): ShortLink {
        return shortLinkManager.update(
            ShortLinkUser(request.username),
            ShortCode(request.code),
            URL(request.url)
        )
    }

    fun handle(request: ExpiryRequest): ShortLink {
        return shortLinkManager.update(
            ShortLinkUser(request.username),
            ShortCode(request.code),
            request.expiresAt,
        )
    }

    data class UrlRequest(val username: String, val code: String, val url: String)

    data class ExpiryRequest(val username: String, val code: String, val expiresAt: Long?)
}
