package ca.jois.shortlink.manager

import ca.jois.shortlink.generator.ShortCodeGenerator
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkStore
import java.net.URL
import java.time.Clock
import kotlinx.coroutines.runBlocking

context(Clock)
class RealShortLinkManager(
    private val shortCodeGenerator: ShortCodeGenerator,
    private val shortLinkStore: ShortLinkStore,
) : ShortLinkManager {
    override fun create(url: URL, expiresAt: Long?, creator: ShortLinkUser): ShortLink {
        val shortCode = shortCodeGenerator.generate()
        val now = millis()
        val shortLink =
            ShortLink(
                creator = creator,
                owner = creator,
                url = url,
                code = shortCode,
                createdAt = now,
                expiresAt = expiresAt
            )

        return runBlocking { shortLinkStore.create(shortLink) }
    }

    override fun get(code: ShortCode) = runBlocking { shortLinkStore.get(code) }

    override fun update(code: ShortCode, url: URL, updater: ShortLinkUser) = runBlocking {
        shortLinkStore.update(code, url, updater)
        shortLinkStore.get(code)!!
    }

    override fun update(code: ShortCode, expiresAt: Long?, updater: ShortLinkUser) = runBlocking {
        shortLinkStore.update(code, expiresAt, updater)
        shortLinkStore.get(code)!!
    }

    override fun delete(code: ShortCode, deleter: ShortLinkUser) = runBlocking {
        shortLinkStore.delete(code, deleter)
    }
}
