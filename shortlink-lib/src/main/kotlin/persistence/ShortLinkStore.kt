package persistence

import model.ShortLink

interface ShortLinkStore {
    fun save(shortLink: ShortLink)

    class DuplicateShortCodeException(code: String) :
        RuntimeException("Link with code $code already exists")
}
