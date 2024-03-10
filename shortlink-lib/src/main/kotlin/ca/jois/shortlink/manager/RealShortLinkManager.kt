package ca.jois.shortlink.manager

import ca.jois.shortlink.generator.ShortCodeGenerator
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkStore
import ca.jois.shortlink.persistence.ShortLinkStore.*
import kotlinx.coroutines.runBlocking
import java.net.URL
import java.time.Clock

context(Clock)
class RealShortLinkManager(
  private val shortCodeGenerator: ShortCodeGenerator,
  private val shortLinkStore: ShortLinkStore,
) : ShortLinkManager {
  override fun listByGroupAndOwner(
    group: ShortLinkGroup,
    owner: ShortLinkUser,
    paginationKey: String?
  ): PaginatedResult<ShortLink> {
    return runBlocking { shortLinkStore.listByGroupAndOwner(group, owner, paginationKey) }
  }

  override fun create(
    url: URL,
    expiresAt: Long?,
    creator: ShortLinkUser,
    group: ShortLinkGroup
  ): ShortLink {
    val shortCode = shortCodeGenerator.generate()
    val now = millis()
    val shortLink =
      ShortLink(
        url = url,
        code = shortCode,
        group = group,
        creator = creator,
        owner = creator,
        createdAt = now,
        expiresAt = expiresAt,
      )

    return runBlocking { shortLinkStore.create(shortLink) }
  }

  override fun get(code: ShortCode, group: ShortLinkGroup) = runBlocking {
    shortLinkStore.get(code, group)
  }

  override fun update(code: ShortCode, url: URL, group: ShortLinkGroup, updater: ShortLinkUser) =
    runBlocking {
      shortLinkStore.update(code, url, group, updater)
      shortLinkStore.get(code, group)!!
    }

  override fun update(
    code: ShortCode,
    expiresAt: Long?,
    group: ShortLinkGroup,
    updater: ShortLinkUser
  ) = runBlocking {
    shortLinkStore.update(code, expiresAt, group, updater)
    shortLinkStore.get(code, group)!!
  }

  override fun delete(code: ShortCode, group: ShortLinkGroup, deleter: ShortLinkUser) =
    runBlocking {
      shortLinkStore.delete(code, group, deleter)
    }
}
