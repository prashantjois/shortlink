package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import java.net.URL

class UpdateShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: UrlRequest): ShortLink {
        return shortLinkManager.update(ShortCode(request.code), URL(request.url))
    }

    fun handle(request: ExpiryRequest): ShortLink {
        return shortLinkManager.update(
            ShortCode(request.code),
            request.expiresAt,
        )
    }

    data class UrlRequest(val code: String, val url: String)

    data class ExpiryRequest(val code: String, val expiresAt: Long?)
}
