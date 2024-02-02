package ca.jois.shortlink.manager

import ca.jois.shortlink.generator.ShortCodeGenerator
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.persistence.ShortLinkStore
import java.net.URL
import java.time.Clock
import kotlinx.coroutines.runBlocking

context(Clock)
class RealShortLinkManager(
    private val shortCodeGenerator: ShortCodeGenerator,
    private val shortLinkStore: ShortLinkStore,
) : ShortLinkManager {
    override fun create(url: URL, expiresAt: Long?): ShortLink {
        val shortCode = shortCodeGenerator.generate()
        val now = millis()
        val shortLink = ShortLink(url, shortCode, now, expiresAt)

        return runBlocking { shortLinkStore.create(shortLink) }
    }

    override fun get(code: ShortCode) = runBlocking { shortLinkStore.get(code) }

    override fun update(code: ShortCode, url: URL) = runBlocking {
        shortLinkStore.update(code, url)
        shortLinkStore.get(code)!!
    }

    override fun update(code: ShortCode, expiresAt: Long?) = runBlocking {
        shortLinkStore.update(code, expiresAt)
        shortLinkStore.get(code)!!
    }

    override fun delete(code: ShortCode) = runBlocking { shortLinkStore.delete(code) }
}
