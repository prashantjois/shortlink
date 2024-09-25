package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL

class CreateShortLinkAction(private val shortLinkManager: ShortLinkManager) {
  fun handle(request: Request): ShortLink {
    val url = URL(request.url)
    val user = ShortLinkUser(request.creator)
    val group = ShortLinkGroup(request.group)
    val expiresAt = request.expiresAt

    if (request.code != null) {
      return shortLinkManager.create(
        shortCode = ShortCode(request.code),
        url = url,
        expiresAt = expiresAt,
        creator = user,
        group = group,
      )
    }

    return shortLinkManager.create(
      url = url,
      expiresAt = expiresAt,
      creator = user,
      group = group,
    )
  }

  data class Request(
    val code: String? = null,
    val creator: String,
    val group: String,
    val url: String,
    val expiresAt: Long? = null,
  )
}
