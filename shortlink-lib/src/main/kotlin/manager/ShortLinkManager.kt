package manager

import java.net.URL
import model.ShortCode
import model.ShortLink

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
     * @return The created [ShortLink] object representing the shortened URL.
     */
    fun create(url: URL, expiresAt: Long? = null): ShortLink

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
     * @param code The [ShortCode] representing the short link to be updated.
     * @param url The new URL the code should point to
     * @return The updated [ShortLink] object.
     */
    fun update(code: ShortCode, url: URL): ShortLink

    /**
     * Updates the expirty associated with the provided [ShortCode].
     *
     * @param code The [ShortCode] representing the short link to be updated.
     * @param expiresAt A new expiration timestamp for the short link (in milliseconds since epoch).
     *   If null, updates the code to not expire.
     * @return The updated [ShortLink] object.
     */
    fun update(code: ShortCode, expiresAt: Long?): ShortLink

    /**
     * Deletes a short link using its unique [ShortCode].
     *
     * @param code The [ShortCode] representing the short link to be deleted.
     */
    fun delete(code: ShortCode)
}
