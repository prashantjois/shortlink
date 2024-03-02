package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.testhelpers.clock.TestClock
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import ca.jois.shortlink.testhelpers.factory.UrlFactory
import kotlin.reflect.KClass
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
    fun `list by owners should get all the shortlinks for a given owner`() = runTest {
        val owner = ShortLinkUser("Someone")
        val shortLinksForOwner = (1..5).map { createDirect(ShortLinkFactory.build(owner = owner)) }
        val otherShortLinks = (1..5).map { createDirect(ShortLinkFactory.build()) }

        shortLinkStore.listByOwner(owner).let {
            assertThat(it.entries).containsExactlyInAnyOrder(*shortLinksForOwner.toTypedArray())
            assertThat(it.entries).doesNotContain(*otherShortLinks.toTypedArray())
        }
    }

    @Test
    fun `list by owners should return paginated results`() = runTest {
        val owner = ShortLinkUser("Someone")
        val shortLinksForOwner = (1..5).map { createDirect(ShortLinkFactory.build(owner = owner)) }
        val otherShortLinks = (1..5).map { createDirect(ShortLinkFactory.build()) }

        val paginationKey =
            shortLinkStore.listByOwner(owner, limit = 3).let {
                assertThat(it.entries).hasSize(3)
                assertThat(shortLinksForOwner).contains(*it.entries.toTypedArray())
                assertThat(otherShortLinks).doesNotContain(*it.entries.toTypedArray())
                assertThat(it.paginationKey).isNotNull()
                it.paginationKey
            }

        shortLinkStore.listByOwner(owner, limit = 3, paginationKey = paginationKey).let {
            assertThat(it.entries).hasSize(2)
            assertThat(shortLinksForOwner).contains(*it.entries.toTypedArray())
            assertThat(otherShortLinks).doesNotContain(*it.entries.toTypedArray())
            assertThat(it.paginationKey).isNull()
        }
    }

    @Test
    fun `shortlink should be saved`() = runTest {
        val shortLink = ShortLinkFactory.build()
        shortLinkStore.create(shortLink)

        getDirect(shortLink.code).let { assertThat(it).isNotNull }
    }

    @Test
    fun `attempting to save shortlink with duplicate short code should throw an exception`() =
        runTest {
            assertExceptionThrown(ShortLinkStore.DuplicateShortCodeException::class) {
                val shortLink = ShortLinkFactory.build()
                repeat(2) { shortLinkStore.create(shortLink) }
            }
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

            shortLinkStore.update(code, url = newUrl, updater = ShortLinkUser.ANONYMOUS)
            getDirect(code)!!.let { assertThat(it.url).isEqualTo(newUrl) }
            shortLinkStore.update(code, expiresAt = newExpiresAt, updater = ShortLinkUser.ANONYMOUS)
            getDirect(code)!!.let { assertThat(it.expiresAt).isEqualTo(newExpiresAt) }
        }
    }

    @Test
    fun `An exception should be thrown when trying to update a shortlink if the code does not exist`() =
        runTest {
            assertExceptionThrown(ShortLinkStore.NotFoundOrNotPermittedException::class) {
                shortLinkStore.update(
                    ShortCode("Something"),
                    url = UrlFactory.random(),
                    updater = ShortLinkUser.ANONYMOUS
                )
            }
        }

    @Test
    fun `An exception should be thrown when trying to update a shortlink if the owner does not match`() =
        runTest {
            val owner = ShortLinkUser("Someone")
            createDirect(ShortLinkFactory.build(owner = owner))

            assertExceptionThrown(ShortLinkStore.NotFoundOrNotPermittedException::class) {
                shortLinkStore.update(
                    ShortCode("Something"),
                    url = UrlFactory.random(),
                    updater = ShortLinkUser.ANONYMOUS
                )
            }
            assertExceptionThrown(ShortLinkStore.NotFoundOrNotPermittedException::class) {
                shortLinkStore.update(
                    ShortCode("Something"),
                    url = UrlFactory.random(),
                    updater = ShortLinkUser("Someone Else"),
                )
            }
        }

    @Test
    fun `Existing entry should be deleted`() = runTest {
        val shortLink = createDirect()
        val code = shortLink.code

        shortLinkStore.delete(code, deleter = ShortLinkUser.ANONYMOUS)

        getDirect(shortLink.code).let { assertThat(it).isNull() }
    }

    @Test
    fun `An exception should be thrown when trying to delete a shortlink if the code does not exist`() =
        runTest {
            assertExceptionThrown(ShortLinkStore.NotFoundOrNotPermittedException::class) {
                shortLinkStore.delete(ShortCode("Missing"), deleter = ShortLinkUser.ANONYMOUS)
            }
        }

    @Test
    fun `An exception should be thrown when trying to delete a shortlink if the owner does not match`() =
        runTest {
            val owner = ShortLinkUser("Someone")
            createDirect(ShortLinkFactory.build(owner = owner))

            assertExceptionThrown(ShortLinkStore.NotFoundOrNotPermittedException::class) {
                shortLinkStore.delete(ShortCode("Missing"), deleter = ShortLinkUser.ANONYMOUS)
            }
            assertExceptionThrown(ShortLinkStore.NotFoundOrNotPermittedException::class) {
                shortLinkStore.delete(ShortCode("Missing"), deleter = ShortLinkUser("Someone Else"))
            }
        }

    private suspend fun assertExceptionThrown(type: KClass<*>, block: suspend () -> Unit) {
        var exception: Exception? = null
        try {
            block()
        } catch (e: Exception) {
            exception = e
        }

        assertThat(exception).isNotNull()
        assertThat(exception).isInstanceOf(type.java)
    }
}
