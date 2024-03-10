package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL

class UpdateShortLinkAction(private val shortLinkManager: ShortLinkManager) {
  fun handle(request: UrlRequest): ShortLink {
    return shortLinkManager.update(
      group = ShortLinkGroup(request.group),
      code = ShortCode(request.code),
      url = URL(request.url),
      updater = ShortLinkUser(request.username),
    )
  }

  fun handle(request: ExpiryRequest): ShortLink {
    return shortLinkManager.update(
      group = ShortLinkGroup(request.group),
      code = ShortCode(request.code),
      expiresAt = request.expiresAt,
      updater = ShortLinkUser(request.username),
    )
  }

  data class UrlRequest(
    val group: String,
    val username: String,
    val code: String,
    val url: String,
  )

  data class ExpiryRequest(
    val group: String,
    val username: String,
    val code: String,
    val expiresAt: Long?,
  )
}
