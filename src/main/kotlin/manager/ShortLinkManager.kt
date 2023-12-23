package manager

import java.net.URL

interface ShortLinkManager {
  fun create(
    url: URL,
    expiresAt: Long? = null
  )
}