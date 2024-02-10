package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL
import java.time.Clock

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
     * @param updater An optional user who is updating the short link. This will be used to
     *   determine whether the user has permission to update the short link. If not provided, only
     *   shortlinks with no owner can be updated.
     * @param code The [ShortCode] representing the short link to be updated.
     * @param url The new URL the code should point to
     * @throws NotFoundOrNotPermittedException if the specified short code does not exist.
     */
    suspend fun update(updater: ShortLinkUser?, code: ShortCode, url: URL)

    /**
     * Updates the expiry associated with the provided [ShortCode].
     *
     * @param updater An optional user who is updating the short link. This will be used to
     *   determine whether the user has permission to update the short link. If not provided, only
     *   shortlinks with no owner can be updated.
     * @param code The [ShortCode] representing the short link to be updated.
     * @param expiresAt A new expiration timestamp for the short link (in milliseconds since epoch).
     *   If null, updates the code to not expire.
     * @return The updated [ShortLink] object.
     * @throws NotFoundOrNotPermittedException if the specified short code does not exist.
     */
    suspend fun update(updater: ShortLinkUser?, code: ShortCode, expiresAt: Long?)

    /**
     * Deletes a short link using its unique [ShortCode].
     *
     * @param deleter An optional user who is deleting the short link. This will be used to
     *   determine whether the user has permission to delete the short link. If not provided, only
     *   shortlinks with no owner can be deleted.
     * @param code The [ShortCode] representing the short link to be deleted.
     * @throws NotFoundOrNotPermittedException if the specified short code does not exist.
     */
    suspend fun delete(deleter: ShortLinkUser?, code: ShortCode)

    class DuplicateShortCodeException(code: ShortCode) :
        RuntimeException("Link with code ${code.value} already exists")

    class NotFoundOrNotPermittedException(code: ShortCode) :
        RuntimeException(
            "ShortLink with code ${code.value} not found or user not permitted to modify."
        )
}
