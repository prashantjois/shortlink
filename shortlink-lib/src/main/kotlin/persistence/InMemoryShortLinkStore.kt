package persistence

import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import model.ShortCode
import model.ShortLink

/** A very simple in-memory storage for Short Links. Not safe for multiprocess applications. */
class InMemoryShortLinkStore : ShortLinkStore {
    private val mutex = Mutex()
    private val shortLinksByCode = HashMap<ShortCode, ShortLink>()

    override suspend fun create(shortLink: ShortLink): ShortLink {
        mutex.withLock {
            val code = shortLink.code
            if (shortLinksByCode.containsKey(code)) {
                throw ShortLinkStore.DuplicateShortCodeException(code.code)
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

    override suspend fun update(code: ShortCode, modify: (ShortLink) -> ShortLink): ShortLink {
        return mutex.withLock {
            val shortLink = shortLinksByCode[code] ?: throw ShortLinkStore.NotFoundException(code)
            val modifiedShortLink = modify(shortLink)

            if (modifiedShortLink.code != code) {
                throw ShortLinkStore.IllegalUpdateException(code, modifiedShortLink.code)
            }
            shortLinksByCode[code] = modifiedShortLink
            modifiedShortLink
        }
    }

    override suspend fun delete(code: ShortCode) {
        mutex.withLock {
            shortLinksByCode[code] ?: throw ShortLinkStore.NotFoundException(code)
            shortLinksByCode.remove(code)
        }
    }
}
