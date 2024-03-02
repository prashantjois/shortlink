package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLinkUser

class DeleteShortLinkAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: Request) {
        shortLinkManager.delete(ShortCode(request.code), ShortLinkUser(request.username))
    }

    data class Request(
        val username: String,
        val code: String,
    )
}
