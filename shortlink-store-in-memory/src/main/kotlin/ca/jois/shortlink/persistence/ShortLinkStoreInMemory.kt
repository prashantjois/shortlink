package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL
import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A very simple in-memory storage for Short Links. Not safe for multiprocess applications. */
open class ShortLinkStoreInMemory : ShortLinkStore {
    private val mutex = Mutex()
    protected val shortLinksByCode = HashMap<ShortCode, ShortLink>()

    override suspend fun listByOwner(
        owner: ShortLinkUser,
        paginationKey: String?,
        limit: Int?,
    ): ShortLinkStore.PaginatedResult<ShortLink> {
        val allShortLinksByOwner =
            shortLinksByCode.values.sortedBy { it.createdAt }.filter { it.owner == owner }

        val totalNumEntries = allShortLinksByOwner.size
        val startIndex = paginationKey?.toIntOrNull() ?: 0
        val limitOrDefault = limit ?: PAGE_SIZE
        val endIndex =
            when (startIndex + limitOrDefault > totalNumEntries) {
                true -> totalNumEntries
                false -> startIndex + limitOrDefault
            }
        val results = allShortLinksByOwner.subList(startIndex, endIndex)

        val nextPaginationKey =
            when (endIndex < allShortLinksByOwner.size - 1) {
                true -> endIndex.toString()
                false -> null
            }
        return ShortLinkStore.PaginatedResult(results, nextPaginationKey)
    }

    override suspend fun create(shortLink: ShortLink): ShortLink {
        mutex.withLock {
            val code = shortLink.code
            if (shortLinksByCode.containsKey(code)) {
                throw ShortLinkStore.DuplicateShortCodeException(code)
            }
            shortLinksByCode[code] = shortLink
            return shortLink
        }
    }

    context(Clock)
    override suspend fun get(code: ShortCode, excludeExpired: Boolean): ShortLink? {
        val shortLink = shortLinksByCode[code] ?: return null

        if (!excludeExpired || shortLink.doesNotExpire()) return shortLink

        return when (shortLink.isExpired()) {
            true -> null
            false -> shortLink
        }
    }

    override suspend fun update(code: ShortCode, url: URL, updater: ShortLinkUser) {
        update(code, updater) { it.copy(url = url) }
    }

    override suspend fun update(code: ShortCode, expiresAt: Long?, updater: ShortLinkUser) {
        update(code, updater) { it.copy(expiresAt = expiresAt) }
    }

    override suspend fun delete(code: ShortCode, deleter: ShortLinkUser) {
        mutex.withLock {
            val shortLink =
                shortLinksByCode[code] ?: throw ShortLinkStore.NotFoundOrNotPermittedException(code)
            if (shortLink.owner != ShortLinkUser.ANONYMOUS && shortLink.owner != deleter) {
                throw ShortLinkStore.NotFoundOrNotPermittedException(code)
            }
            shortLinksByCode.remove(code)
        }
    }

    private suspend fun update(
        code: ShortCode,
        updater: ShortLinkUser,
        modify: (ShortLink) -> ShortLink
    ): ShortLink {
        return mutex.withLock {
            val shortLink =
                shortLinksByCode[code] ?: throw ShortLinkStore.NotFoundOrNotPermittedException(code)
            if (shortLink.owner != ShortLinkUser.ANONYMOUS && shortLink.owner != updater) {
                throw ShortLinkStore.NotFoundOrNotPermittedException(code)
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
