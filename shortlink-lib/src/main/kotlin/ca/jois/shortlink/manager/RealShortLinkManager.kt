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
    override fun create(creator: ShortLinkUser?, url: URL, expiresAt: Long?): ShortLink {
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

    override fun update(updater: ShortLinkUser?, code: ShortCode, url: URL) = runBlocking {
        shortLinkStore.update(updater, code, url)
        shortLinkStore.get(code)!!
    }

    override fun update(updater: ShortLinkUser?, code: ShortCode, expiresAt: Long?) = runBlocking {
        shortLinkStore.update(updater, code, expiresAt)
        shortLinkStore.get(code)!!
    }

    override fun delete(deleter: ShortLinkUser?, code: ShortCode) = runBlocking {
        shortLinkStore.delete(deleter, code)
    }
}
