package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.testhelpers.clock.TestClock

class ShortLinkStoreInMemoryTest : ShortLinkStoreTest {
  override val shortLinkStore: ShortLinkStore = ShortLinkStoreInMemory()

  override suspend fun getDirect(code: ShortCode, group: ShortLinkGroup): ShortLink? {
    with(TestClock()) {
      return shortLinkStore.get(code, group)
    }
  }

  override suspend fun createDirect(shortLink: ShortLink): ShortLink {
    return shortLinkStore.create(shortLink)
  }
}
