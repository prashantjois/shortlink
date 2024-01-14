package testhelpers.factory

import generator.NaiveShortCodeGenerator
import java.net.URL
import model.ShortCode
import model.ShortLink

object ShortLinkFactory {
    /** Generates a [ShortLink] with the given parameters, choosing defaults if not specified. */
    fun build(
        originalUrl: URL = UrlFactory.random(),
        shortCode: ShortCode = NaiveShortCodeGenerator().generate(),
        createdAt: Long = 0,
        expiresAt: Long? = null,
    ) = ShortLink(url = originalUrl, code = shortCode, createdAt = createdAt, expiresAt = expiresAt)
}
