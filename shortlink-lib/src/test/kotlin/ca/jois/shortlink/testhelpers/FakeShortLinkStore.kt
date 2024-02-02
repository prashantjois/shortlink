package ca.jois.shortlink.testhelpers

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.persistence.ShortLinkStore
import java.net.URL
import java.time.Clock

class FakeShortLinkStore : ShortLinkStore {
    private val shortLinksByCode = HashMap<ShortCode, ShortLink>()

    override suspend fun create(shortLink: ShortLink): ShortLink {
        shortLinksByCode[shortLink.code] = shortLink
        return shortLink
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

    override suspend fun update(code: ShortCode, url: URL) {
        update(code) { it.copy(url = url) }
    }

    override suspend fun update(code: ShortCode, expiresAt: Long?) {
        update(code) { it.copy(expiresAt = expiresAt) }
    }

    override suspend fun delete(code: ShortCode) {
        shortLinksByCode.remove(code)
    }

    private fun update(code: ShortCode, update: (ShortLink) -> ShortLink) {
        val shortLink = update(shortLinksByCode[code]!!)
        shortLinksByCode[code] = shortLink
    }
}
