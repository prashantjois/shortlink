package manager

import generator.ShortCodeGenerator
import java.net.URL
import kotlinx.coroutines.runBlocking
import model.ShortLink
import persistence.ShortLinkStore

class RealShortLinkManager(
    private val shortCodeGenerator: ShortCodeGenerator,
    private val shortLinkStore: ShortLinkStore,
) : ShortLinkManager {
    override fun create(url: URL, expiresAt: Long?) {
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                val shortCode = shortCodeGenerator.generate()
                val now = System.currentTimeMillis()
                val shortLink = ShortLink(url, shortCode, now, expiresAt)

                runBlocking { shortLinkStore.save(shortLink) }
                break
            } catch (e: ShortLinkStore.DuplicateShortCodeException) {
                // In case of collision, try again with a new short code
            }
        }

        // We've likely exhausted the pool of available codes or some other bug is causing
        // collisions.
        throw ExhaustedCodePoolException()
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
    }

    class ExhaustedCodePoolException :
        RuntimeException(
            "All attempts to persist randomly generated short code failed. Available pool of codes may have run out."
        )
}
