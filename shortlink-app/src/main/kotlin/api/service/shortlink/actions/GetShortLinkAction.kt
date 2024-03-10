package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup

class GetShortLinkAction(private val shortLinkManager: ShortLinkManager) {
  fun handle(request: Request): ShortLink? {
    return shortLinkManager.get(ShortCode(request.code), ShortLinkGroup(request.group))
  }

  data class Request(
    val group: String,
    val code: String,
  )
}
