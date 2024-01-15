package manager

import java.net.URL
import model.ShortLink

interface ShortLinkManager {
    fun create(url: URL, expiresAt: Long? = null): ShortLink
}
