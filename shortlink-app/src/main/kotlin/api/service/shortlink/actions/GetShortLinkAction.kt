package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink

class GetShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: Request): ShortLink? {
        return shortLinkManager.get(ShortCode(request.code))
    }

    data class Request(val code: String)
}
