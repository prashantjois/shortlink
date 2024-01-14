package persistence

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import persistence.ShortLinkStore.DuplicateShortCodeException
import testhelpers.clock.TestClock
import testhelpers.factory.ShortLinkFactory
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class InMemoryShortLinkStoreTest {
    private val shortLinkStore = InMemoryShortLinkStore

    @Test
    fun `shortlink should be saved`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build()
            shortLinkStore.save(shortLink)

            assertThat(shortLinkStore.get(shortLink.code)).isNotNull
        }
    }

    @Test
    fun `attempting to save shortlink with duplicate short code should throw an exception`() = runTest {
        val shortLink = ShortLinkFactory.build()
        var exception: DuplicateShortCodeException? = null
        try {
            repeat(2) { shortLinkStore.save(shortLink) }
        } catch (e: DuplicateShortCodeException) {
            exception = e
        }
        assertThat(exception).isNotNull()
    }

    @Test
    fun `get() should retrieve a previously created shortlink`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build()

            shortLinkStore.save(shortLink)

            with(shortLinkStore.get(shortLink.code)!!) {
                assertThat(url).isEqualTo(shortLink.url)
                assertThat(code).isEqualTo(shortLink.code)
                assertThat(createdAt).isEqualTo(shortLink.createdAt)
                assertThat(expiresAt).isEqualTo(shortLink.expiresAt)
            }
        }
    }

    @Test
    fun `get() should return null if a shortlink exists but is expired`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())

            shortLinkStore.save(shortLink)

            advanceClockBy(5.minutes)

            assertThat(shortLinkStore.get(shortLink.code)).isNotNull

            advanceClockBy(1.seconds)

            assertThat(shortLinkStore.get(shortLink.code)).isNull()
        }
    }
}