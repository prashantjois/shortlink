package shortlinkapp.api.service.shortlink.actions

import manager.ShortLinkManager
import model.ShortCode
import model.ShortLink

class GetShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: Request): ShortLink? {
        return shortLinkManager.get(ShortCode(request.code))
    }

    data class Request(val code: String)
}
