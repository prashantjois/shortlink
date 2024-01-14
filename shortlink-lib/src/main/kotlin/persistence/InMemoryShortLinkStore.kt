package persistence

import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import model.ShortCode
import model.ShortLink

/** A very simple in-memory storage for Short Links. Not safe for multiprocess applications. */
object InMemoryShortLinkStore : ShortLinkStore {
    private val mutex = Mutex()
    private val shortLinksByCode = HashMap<ShortCode, ShortLink>()

    override suspend fun save(shortLink: ShortLink) {
        mutex.withLock {
            val shortCode = shortLink.code
            if (shortLinksByCode.containsKey(shortCode)) {
                throw ShortLinkStore.DuplicateShortCodeException(shortCode.code)
            }
            shortLinksByCode[shortCode] = shortLink
        }
    }

    context(Clock)
    override suspend fun get(shortCode: ShortCode, excludeExpired: Boolean): ShortLink? {
        val shortLink = shortLinksByCode[shortCode] ?: return null

        shortLink.expiresAt?.let { expiresAt ->
            if (instant().toEpochMilli() > expiresAt) return null
        }

        return shortLink
    }
}
