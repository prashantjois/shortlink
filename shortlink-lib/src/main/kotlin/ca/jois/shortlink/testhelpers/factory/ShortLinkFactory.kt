package ca.jois.shortlink.testhelpers.factory

import ca.jois.shortlink.generator.NaiveShortCodeGenerator
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL

object ShortLinkFactory {
  /** Generates a [ShortLink] with the given parameters, choosing defaults if not specified. */
  fun build(
    code: ShortCode = NaiveShortCodeGenerator().generate(),
    group: ShortLinkGroup = ShortLinkGroup.DEFAULT,
    creator: ShortLinkUser = ShortLinkUser.ANONYMOUS,
    owner: ShortLinkUser = creator,
    originalUrl: URL = UrlFactory.random(),
    createdAt: Long = 0,
    expiresAt: Long? = null,
  ) =
    ShortLink(
      group = group,
      creator = creator,
      owner = owner,
      url = originalUrl,
      code = code,
      createdAt = createdAt,
      expiresAt = expiresAt,
    )
}
