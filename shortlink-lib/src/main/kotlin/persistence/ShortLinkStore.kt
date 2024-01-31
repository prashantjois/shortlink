package persistence

import java.net.URL
import java.time.Clock
import model.ShortCode
import model.ShortLink

interface ShortLinkStore {
    /**
     * Persists the given [shortLink] in a thread-safe manner.
     *
     * @throws [DuplicateShortCodeException] if an entry already exists with the given
     *   [ShortLink.code]
     */
    suspend fun create(shortLink: ShortLink): ShortLink

    /**
     * Retrieves the [ShortLink] associated with the given [code]. Returns null if no [ShortLink] is
     * associated to the code, or if the [ShortLink] entry is expired.
     *
     * @param code The short code used to reference the short link
     * @param excludeExpired Return null if a matching entry exists but is expired
     */
    context(Clock)
    suspend fun get(code: ShortCode, excludeExpired: Boolean = true): ShortLink?

    /**
     * Updates the URL associated with the provided [ShortCode].
     *
     * @param code The [ShortCode] representing the short link to be updated.
     * @param url The new URL the code should point to
     * @throws NotFoundException if the specified short code does not exist.
     */
    suspend fun update(code: ShortCode, url: URL)

    /**
     * Updates the expiry associated with the provided [ShortCode].
     *
     * @param code The [ShortCode] representing the short link to be updated.
     * @param expiresAt A new expiration timestamp for the short link (in milliseconds since epoch).
     *   If null, updates the code to not expire.
     * @return The updated [ShortLink] object.
     * @throws NotFoundException if the specified short code does not exist.
     */
    suspend fun update(code: ShortCode, expiresAt: Long?)

    /**
     * Deletes a short link using its unique [ShortCode].
     *
     * @param code The [ShortCode] representing the short link to be deleted.
     * @throws NotFoundException if the specified short code does not exist.
     */
    suspend fun delete(code: ShortCode)

    class DuplicateShortCodeException(code: String) :
        RuntimeException("Link with code $code already exists")

    class NotFoundException(code: ShortCode) :
        RuntimeException("ShortLink with code $code not found.")
}
