package manager

import java.net.URL
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
}
