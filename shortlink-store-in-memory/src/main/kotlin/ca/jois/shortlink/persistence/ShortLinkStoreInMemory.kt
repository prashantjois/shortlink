package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkStore.PaginatedResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URL
import java.time.Clock

/** A very simple in-memory storage for Short Links. Not safe for multiprocess applications. */
open class ShortLinkStoreInMemory : ShortLinkStore {
  private val mutex = Mutex()
  protected val shortLinksByGroupAndCode =
    HashMap<ShortLinkGroup, HashMap<ShortCode, ShortLink>>()

  override suspend fun listByGroupAndOwner(
    group: ShortLinkGroup,
    owner: ShortLinkUser,
    paginationKey: String?,
    limit: Int?,
  ): PaginatedResult<ShortLink> {
    val shortLinksByCode =
      shortLinksByGroupAndCode[group] ?: return PaginatedResult(emptyList(), null)

    val shortLinksByOwner =
      shortLinksByCode.values.sortedBy { it.createdAt }.filter { it.owner == owner }

    val totalNumEntries = shortLinksByOwner.size
    val startIndex = paginationKey?.toIntOrNull() ?: 0
    val limitOrDefault = limit ?: PAGE_SIZE
    val endIndex =
      when (startIndex + limitOrDefault > totalNumEntries) {
        true -> totalNumEntries
        false -> startIndex + limitOrDefault
      }
    val results = shortLinksByOwner.subList(startIndex, endIndex)

    val nextPaginationKey =
      when (endIndex < shortLinksByOwner.size - 1) {
        true -> endIndex.toString()
        false -> null
      }
    return PaginatedResult(results, nextPaginationKey)
  }

  override suspend fun create(shortLink: ShortLink): ShortLink {
    mutex.withLock {
      val code = shortLink.code
      val shortLinksByCode = shortLinksByGroupAndCode[shortLink.group] ?: HashMap()
      if (shortLinksByCode.containsKey(code)) {
        throw ShortLinkStore.DuplicateShortCodeException(shortLink.group, code)
      }
      shortLinksByCode[code] = shortLink
      shortLinksByGroupAndCode[shortLink.group] = shortLinksByCode
      return shortLink
    }
  }

  context(Clock)
  override suspend fun get(
    code: ShortCode,
    group: ShortLinkGroup,
    excludeExpired: Boolean
  ): ShortLink? {
    val shortLinksByCode = shortLinksByGroupAndCode[group] ?: return null
    val shortLink = shortLinksByCode[code] ?: return null

    if (!excludeExpired || shortLink.doesNotExpire()) return shortLink

    return when (shortLink.isExpired()) {
      true -> null
      false -> shortLink
    }
  }

  override suspend fun update(
    code: ShortCode,
    url: URL,
    group: ShortLinkGroup,
    updater: ShortLinkUser
  ) {
    update(code, group, updater) { it.copy(url = url) }
  }

  override suspend fun update(
    code: ShortCode,
    expiresAt: Long?,
    group: ShortLinkGroup,
    updater: ShortLinkUser
  ) {
    update(code, group, updater) { it.copy(expiresAt = expiresAt) }
  }

  override suspend fun delete(code: ShortCode, group: ShortLinkGroup, deleter: ShortLinkUser) {
    mutex.withLock {
      val shortLinksByCode =
        shortLinksByGroupAndCode[group]
          ?: throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
      val shortLink =
        shortLinksByCode[code]
          ?: throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)

      if (shortLink.owner != ShortLinkUser.ANONYMOUS && shortLink.owner != deleter) {
        throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
      }

      shortLinksByCode.remove(code)
    }
  }

  private suspend fun update(
    code: ShortCode,
    group: ShortLinkGroup,
    updater: ShortLinkUser,
    modify: (ShortLink) -> ShortLink
  ): ShortLink {
    return mutex.withLock {
      val shortLinksByCode =
        shortLinksByGroupAndCode[group]
          ?: throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
      val shortLink =
        shortLinksByCode[code]
          ?: throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
      if (shortLink.owner != ShortLinkUser.ANONYMOUS && shortLink.owner != updater) {
        throw ShortLinkStore.NotFoundOrNotPermittedException(group, code)
      }
      val modifiedShortLink = modify(shortLink)

      shortLinksByCode[code] = modifiedShortLink
      modifiedShortLink
    }
  }

  companion object {
    const val PAGE_SIZE = 100
  }
}
