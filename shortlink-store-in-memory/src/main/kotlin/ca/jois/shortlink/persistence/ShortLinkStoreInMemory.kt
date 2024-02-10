package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL
import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A very simple in-memory storage for Short Links. Not safe for multiprocess applications. */
class ShortLinkStoreInMemory : ShortLinkStore {
    private val mutex = Mutex()
    private val shortLinksByCode = HashMap<ShortCode, ShortLink>()

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

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, url: URL) {
        update(updater, code) { it.copy(url = url) }
    }

    override suspend fun update(updater: ShortLinkUser?, code: ShortCode, expiresAt: Long?) {
        update(updater, code) { it.copy(expiresAt = expiresAt) }
    }

    override suspend fun delete(deleter: ShortLinkUser?, code: ShortCode) {
        mutex.withLock {
            val shortLink =
                shortLinksByCode[code] ?: throw ShortLinkStore.NotFoundOrNotPermittedException(code)
            if (shortLink.owner != null && shortLink.owner != deleter) {
                throw ShortLinkStore.NotFoundOrNotPermittedException(code)
            }
            shortLinksByCode.remove(code)
        }
    }

    private suspend fun update(
        updater: ShortLinkUser?,
        code: ShortCode,
        modify: (ShortLink) -> ShortLink
    ): ShortLink {
        return mutex.withLock {
            val shortLink =
                shortLinksByCode[code] ?: throw ShortLinkStore.NotFoundOrNotPermittedException(code)
            if (shortLink.owner != null && shortLink.owner != updater) {
                throw ShortLinkStore.NotFoundOrNotPermittedException(code)
            }
            val modifiedShortLink = modify(shortLink)

            shortLinksByCode[code] = modifiedShortLink
            modifiedShortLink
        }
    }
}
