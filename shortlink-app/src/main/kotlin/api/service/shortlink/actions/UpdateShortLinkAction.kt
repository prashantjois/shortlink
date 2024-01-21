package shortlinkapp.api.service.shortlink.actions

import java.net.URL
import manager.ShortLinkManager
import model.ShortCode
import model.ShortLink

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
