package manager

import generator.NaiveShortCodeGenerator
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import persistence.InMemoryShortLinkStore
import testhelpers.clock.TestClock
import testhelpers.factory.UrlFactory

class RealShortLinkManagerTest {
    @Nested
    @DisplayName("RealShortLinkManager#create")
    inner class CreateTest {
        @Test
        fun `ShortLink is created with the provided params`() = runTest {
            with(TestClock()) {
                val shortLinkStore = InMemoryShortLinkStore()
                val realShortLinkManager =
                    RealShortLinkManager(
                        shortCodeGenerator = NaiveShortCodeGenerator(),
                        shortLinkStore = shortLinkStore
                    )
                val url = UrlFactory.random()
                val shortLink =
                    realShortLinkManager.create(
                        url = url,
                        expiresAt = 5.minutes.fromNow().toEpochMilli()
                    )
                val createdShortLink = shortLinkStore.get(shortLink.code)
                assertThat(createdShortLink).isEqualTo(shortLink)
            }
        }
    }
}
