package ca.jois.shortlink.testhelpers.factory

import ca.jois.shortlink.generator.NaiveShortCodeGenerator
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import java.net.URL

object ShortLinkFactory {
    /** Generates a [ShortLink] with the given parameters, choosing defaults if not specified. */
    fun build(
        originalUrl: URL = UrlFactory.random(),
        shortCode: ShortCode = NaiveShortCodeGenerator().generate(),
        createdAt: Long = 0,
        expiresAt: Long? = null,
    ) = ShortLink(url = originalUrl, code = shortCode, createdAt = createdAt, expiresAt = expiresAt)
}
