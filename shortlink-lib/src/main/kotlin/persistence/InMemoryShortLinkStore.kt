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
            val shortCode = shortLink.code
            if (shortLinksByCode.containsKey(shortCode)) {
                throw ShortLinkStore.DuplicateShortCodeException(shortCode.code)
            }
            shortLinksByCode[shortCode] = shortLink
            return shortLink
        }
    }

    context(Clock)
    override suspend fun get(shortCode: ShortCode, excludeExpired: Boolean): ShortLink? {
        val shortLink = shortLinksByCode[shortCode] ?: return null

        if (!excludeExpired || shortLink.doesNotExpire()) return shortLink

        return when (shortLink.isExpired()) {
            true -> null
            false -> shortLink
        }
    }
}
