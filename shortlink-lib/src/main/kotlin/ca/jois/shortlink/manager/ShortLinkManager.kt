package ca.jois.shortlink.manager

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL

/**
 * A manager for ShortLink CRUD operations.
 *
 * This interface provides methods to perform operations related to short links, including creation,
 * retrieval, update, and deletion.
 */
interface ShortLinkManager {
    /**
     * Creates a new short link for the given URL.
     *
     * @param url The URL to be shortened.
     * @param expiresAt An optional expiration timestamp for the short link (in milliseconds since
     *   epoch). If not provided, the code will not expire.
     * @param creator An optional user who created the short link.
     * @return The created [ShortLink] object representing the shortened URL.
     */
    fun create(
        url: URL,
        expiresAt: Long? = null,
        creator: ShortLinkUser = ShortLinkUser.ANONYMOUS
    ): ShortLink

    /**
     * Retrieves a short link using its unique [ShortCode].
     *
     * @param code The [ShortCode] representing the short link.
     * @return The [ShortLink] associated with the provided [ShortCode], or null if the short code
     *   does not exist or is expired.
     */
    fun get(code: ShortCode): ShortLink?

    /**
     * Updates the URL associated with the provided [ShortCode].
     *
     * @param updater An optional user who is updating the short link. This will be used to
     *   determine whether the user has permission to update the short link.
     * @param code The [ShortCode] representing the short link to be updated.
     * @param url The new URL the code should point to
     * @return The updated [ShortLink] object.
     */
    fun update(code: ShortCode, url: URL, updater: ShortLinkUser): ShortLink

    /**
     * Updates the expiry associated with the provided [ShortCode].
     *
     * @param updater An optional user who is updating the short link. This will be used to
     *   determine whether the user has permission to update the short link.
     * @param code The [ShortCode] representing the short link to be updated.
     * @param expiresAt A new expiration timestamp for the short link (in milliseconds since epoch).
     *   If null, updates the code to not expire.
     * @return The updated [ShortLink] object.
     */
    fun update(code: ShortCode, expiresAt: Long?, updater: ShortLinkUser): ShortLink

    /**
     * Deletes a short link using its unique [ShortCode].
     *
     * @param deleter An optional user who is deleting the short link. This will be used to
     *   determine whether the user has permission to delete the short link.
     * @param code The [ShortCode] representing the short link to be deleted.
     */
    fun delete(code: ShortCode, deleter: ShortLinkUser)
}
