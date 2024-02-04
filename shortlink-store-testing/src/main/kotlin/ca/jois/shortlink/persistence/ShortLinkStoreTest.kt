package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.testhelpers.clock.TestClock
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import ca.jois.shortlink.testhelpers.factory.UrlFactory
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * A common set of tests that exercise the methods of [ShortLinkStore]. This is provided as an
 * interface to allow concrete implementations to spin up the necessary infrastructure, such as
 * database containers, to exercise the concrete implementations of [ShortLinkStore].
 */
interface ShortLinkStoreTest {
    /**
     * The [ShortLinkStore] instance under test. This should be the concrete implementation of the
     * [ShortLinkStore] that needs testing.
     */
    val shortLinkStore: ShortLinkStore

    /**
     * Concrete implementations of this class should retrieve a [ShortLink] by its short code
     * directly from the database, bypassing any caching or intermediate logic.
     *
     * @param code The unique short code identifying the [ShortLink] to retrieve.
     * @return The [ShortLink] associated with the given code, or `null` if not found.
     */
    suspend fun getDirect(code: ShortCode): ShortLink?

    /**
     * Concrete implementations should implement this to creates a new [ShortLink] directly in the
     * database, bypassing any validation or processing logic outside the database interactions.
     *
     * @param shortLink The [ShortLink] instance to be created.
     * @return The [ShortLink] after it has been inserted into the database, typically including any
     *   modifications made during the insertion process (e.g., generated ID).
     */
    suspend fun createDirect(shortLink: ShortLink = ShortLinkFactory.build()): ShortLink

    @Test
    fun `shortlink should be saved`() = runTest {
        val shortLink = ShortLinkFactory.build()
        shortLinkStore.create(shortLink)

        getDirect(shortLink.code).let { assertThat(it).isNotNull }
    }

    @Test
    fun `attempting to save shortlink with duplicate short code should throw an exception`() =
        runTest {
            val shortLink = ShortLinkFactory.build()
            var exception: ShortLinkStore.DuplicateShortCodeException? = null
            try {
                repeat(2) { shortLinkStore.create(shortLink) }
            } catch (e: ShortLinkStore.DuplicateShortCodeException) {
                exception = e
            }
            assertThat(exception).isNotNull()
        }

    @Test
    fun `retrieve a previously created shortlink`() = runTest {
        with(TestClock()) {
            val shortLink = createDirect()

            with(shortLinkStore.get(shortLink.code)!!) {
                assertThat(url).isEqualTo(shortLink.url)
                assertThat(code).isEqualTo(shortLink.code)
                assertThat(createdAt).isEqualTo(shortLink.createdAt)
                assertThat(expiresAt).isEqualTo(shortLink.expiresAt)
            }
        }
    }

    @Test
    fun `should return null if a shortlink exists but is expired`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
            createDirect(shortLink)

            advanceClockBy(5.minutes)

            assertThat(shortLinkStore.get(shortLink.code)).isNotNull

            advanceClockBy(1.seconds)

            assertThat(shortLinkStore.get(shortLink.code)).isNull()
        }
    }

    @Test
    fun `should return the shortlink if it is expired but excludeExpired is false`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())

            createDirect(shortLink)

            advanceClockBy(6.minutes)

            assertThat(shortLinkStore.get(shortLink.code)).isNull()

            assertThat(shortLinkStore.get(shortLink.code, excludeExpired = false)).isNotNull
        }
    }

    @Test
    fun `Existing entry should be updated with modified parameters`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
            val code = shortLink.code

            createDirect(shortLink)

            val newUrl = UrlFactory.random()
            val newExpiresAt = 6.minutes.fromNow().toEpochMilli()

            shortLinkStore.update(code, url = newUrl)
            getDirect(code)!!.let { assertThat(it.url).isEqualTo(newUrl) }
            shortLinkStore.update(code, expiresAt = newExpiresAt)
            getDirect(code)!!.let { assertThat(it.expiresAt).isEqualTo(newExpiresAt) }
        }
    }

    @Test
    fun `An exception should be thrown if the code does not exist`() = runTest {
        var exception: ShortLinkStore.NotFoundException? = null
        try {
            shortLinkStore.update(ShortCode("Something"), url = UrlFactory.random())
        } catch (e: ShortLinkStore.NotFoundException) {
            exception = e
        }

        assertThat(exception).isNotNull()
    }

    @Test
    fun `Existing entry should be deleted`() = runTest {
        val shortLink = createDirect()
        val code = shortLink.code

        shortLinkStore.delete(code)

        getDirect(shortLink.code).let { assertThat(it).isNull() }
    }

    fun `it should throw an exception if the short link doesn't exist`() = runTest {
        var exception: ShortLinkStore.NotFoundException? = null
        try {
            shortLinkStore.delete(ShortCode("Missing"))
        } catch (e: ShortLinkStore.NotFoundException) {
            exception = e
        }

        assertThat(exception).isNotNull
    }
}
