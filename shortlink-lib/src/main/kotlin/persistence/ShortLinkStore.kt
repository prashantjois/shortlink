package persistence

import java.time.Clock
import model.ShortCode
import model.ShortLink

interface ShortLinkStore {
    /**
     * Persists the given [shortLink] in a thread-safe manner. Throws [DuplicateShortCodeException]
     * contention is detected.
     */
    suspend fun save(shortLink: ShortLink)

    /**
     * Retrieves the [ShortLink] associated with the given [shortCode]. Returns null if no
     * [ShortLink] is associated to the code, or if the [ShortLink] entry is expired.
     *
     * @param shortCode The short code used to reference the short link
     * @param excludeExpired Return null if a matching entry exists but is expired
     */
    context(Clock)
    suspend fun get(shortCode: ShortCode, excludeExpired: Boolean = true): ShortLink?

    class DuplicateShortCodeException(code: String) :
        RuntimeException("Link with code $code already exists")
}
