package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortCode

class DeleteShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: Request) {
        shortLinkManager.delete(ShortCode(request.code))
    }

    data class Request(
        val code: String,
    )
}
