package ca.jois.shortlink.testhelpers

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkStore
import ca.jois.shortlink.persistence.ShortLinkStore.PaginatedResult
import java.net.URL
import java.time.Clock

class ShortLinkStoreFake : ShortLinkStore {
    private val shortLinks = HashMap<ShortLinkGroup, HashMap<ShortCode, ShortLink>>()

    override suspend fun listByOwner(
        group: ShortLinkGroup,
        owner: ShortLinkUser,
        paginationKey: String?,
        limit: Int?,
    ): PaginatedResult<ShortLink> {
        val shortLinksByOwner = shortLinks[group] ?: return PaginatedResult(emptyList(), null)
        return shortLinksByOwner.values
            .filter { it.owner == owner }
            .take(limit ?: Int.MAX_VALUE)
            .let { PaginatedResult(it, null) }
    }

    override suspend fun create(shortLink: ShortLink): ShortLink {
        val shortLinksByOwner = shortLinks[shortLink.group] ?: HashMap()
        shortLinksByOwner[shortLink.code] = shortLink
        shortLinks[shortLink.group] = shortLinksByOwner
        return shortLink
    }

    context(Clock)
    override suspend fun get(
        code: ShortCode,
        group: ShortLinkGroup,
        excludeExpired: Boolean
    ): ShortLink? {
        val shortLinksByOwner = shortLinks[group] ?: return null
        val shortLink = shortLinksByOwner[code] ?: return null

        if (!excludeExpired || shortLink.doesNotExpire()) return shortLink

        return when (shortLink.isExpired()) {
            true -> null
            false -> shortLink
        }
    }

    override suspend fun update(
        code: ShortCode,
        url: URL,
        group: ShortLinkGroup,
        updater: ShortLinkUser
    ) {
        update(code, group) { it.copy(url = url) }
    }

    override suspend fun update(
        code: ShortCode,
        expiresAt: Long?,
        group: ShortLinkGroup,
        updater: ShortLinkUser
    ) {
        update(code, group) { it.copy(expiresAt = expiresAt) }
    }

    override suspend fun delete(code: ShortCode, group: ShortLinkGroup, deleter: ShortLinkUser) {
        val shortLinksByOwner = shortLinks[group] ?: return
        shortLinksByOwner.remove(code)
    }

    private fun update(code: ShortCode, group: ShortLinkGroup, update: (ShortLink) -> ShortLink) {
        val shortLinksByOwner = shortLinks[group] ?: HashMap()
        val shortLink = update(shortLinksByOwner[code]!!)
        shortLinksByOwner[code] = shortLink
    }
}
