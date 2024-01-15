package manager

import generator.ShortCodeGenerator
import java.net.URL
import java.time.Clock
import kotlinx.coroutines.runBlocking
import model.ShortCode
import model.ShortLink
import persistence.ShortLinkStore

context(Clock)
class RealShortLinkManager(
    private val shortCodeGenerator: ShortCodeGenerator,
    private val shortLinkStore: ShortLinkStore,
) : ShortLinkManager {
    override fun create(url: URL, expiresAt: Long?): ShortLink {
        val shortCode = shortCodeGenerator.generate()
        val now = System.currentTimeMillis()
        val shortLink = ShortLink(url, shortCode, now, expiresAt)

        return runBlocking { shortLinkStore.create(shortLink) }
    }

    override fun get(code: ShortCode): ShortLink? {
        return runBlocking { shortLinkStore.get(code) }
    }
}
