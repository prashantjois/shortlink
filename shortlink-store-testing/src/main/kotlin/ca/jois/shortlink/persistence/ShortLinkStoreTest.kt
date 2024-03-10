package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkStore.NotFoundOrNotPermittedException
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
    suspend fun getDirect(code: ShortCode, group: ShortLinkGroup): ShortLink?

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
        val group = ShortLinkGroup.DEFAULT
        val owner = ShortLinkUser("Someone")
        val shortLinksForOwner =
            (1..5).map { createDirect(ShortLinkFactory.build(group = group, owner = owner)) }
        val shortLinksByDifferentOwner =
            (1..5).map {
                createDirect(ShortLinkFactory.build(group = group, owner = ShortLinkUser("other")))
            }
        val shortLinksByDifferentGroup =
            (1..5).map {
                createDirect(ShortLinkFactory.build(group = ShortLinkGroup("other"), owner = owner))
            }

        shortLinkStore.listByGroupAndOwner(group, owner).let {
            assertThat(it.entries).containsExactlyInAnyOrder(*shortLinksForOwner.toTypedArray())
            assertThat(it.entries).doesNotContain(*shortLinksByDifferentOwner.toTypedArray())
            assertThat(it.entries).doesNotContain(*shortLinksByDifferentGroup.toTypedArray())
        }
    }

    @Test
    fun `list by owners should return paginated results`() = runTest {
        val group = ShortLinkGroup.DEFAULT
        val owner = ShortLinkUser("Someone")
        val shortLinksForOwner = (1..5).map { createDirect(ShortLinkFactory.build(owner = owner)) }
        val otherShortLinks = (1..5).map { createDirect(ShortLinkFactory.build()) }

        val paginationKey =
            shortLinkStore.listByGroupAndOwner(group, owner, limit = 3).let {
                assertThat(it.entries).hasSize(3)
                assertThat(shortLinksForOwner).contains(*it.entries.toTypedArray())
                assertThat(otherShortLinks).doesNotContain(*it.entries.toTypedArray())
                assertThat(it.nextPageKey).isNotNull()
                it.nextPageKey
            }

        shortLinkStore.listByGroupAndOwner(group, owner, limit = 3, paginationKey = paginationKey).let {
            assertThat(it.entries).hasSize(2)
            assertThat(shortLinksForOwner).contains(*it.entries.toTypedArray())
            assertThat(otherShortLinks).doesNotContain(*it.entries.toTypedArray())
            assertThat(it.nextPageKey).isNull()
        }
    }

    @Test
    fun `a new shortlink should be created`() = runTest {
        val shortLink = ShortLinkFactory.build()
        shortLinkStore.create(shortLink)

        getDirect(shortLink.code, shortLink.group).let { assertThat(it).isNotNull }
    }

    @Test
    fun `a shortlink with the same code but different group should be allowed`() = runTest {
        val code = ShortCode("same")

        val shortLink = ShortLinkFactory.build(code = code)
        val shortLinkOtherGroup = shortLink.copy(code = code, group = ShortLinkGroup("other"))

        shortLinkStore.create(shortLink)
        shortLinkStore.create(shortLinkOtherGroup)

        getDirect(code, shortLink.group).let { assertThat(it).isNotNull }
        getDirect(code, shortLinkOtherGroup.group).let { assertThat(it).isNotNull }
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

            with(shortLinkStore.get(shortLink.code, shortLink.group)!!) {
                assertThat(url).isEqualTo(shortLink.url)
                assertThat(code).isEqualTo(shortLink.code)
                assertThat(createdAt).isEqualTo(shortLink.createdAt)
                assertThat(expiresAt).isEqualTo(shortLink.expiresAt)
            }
        }
    }

    @Test
    fun `retrieving a shortlink should differentiate by group`() = runTest {
        with(TestClock()) {
            val shortLink = createDirect(ShortLinkFactory.build(group = ShortLinkGroup("group")))

            assertThat(shortLinkStore.get(shortLink.code, shortLink.group)).isNotNull
            assertThat(shortLinkStore.get(shortLink.code, ShortLinkGroup("other"))).isNull()
        }
    }

    @Test
    fun `should return null if a shortlink exists but is expired`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
            createDirect(shortLink)

            advanceClockBy(5.minutes)

            assertThat(shortLinkStore.get(shortLink.code, shortLink.group)).isNotNull

            advanceClockBy(1.seconds)

            assertThat(shortLinkStore.get(shortLink.code, shortLink.group)).isNull()
        }
    }

    @Test
    fun `should return the shortlink if it is expired but excludeExpired is false`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())

            createDirect(shortLink)

            advanceClockBy(6.minutes)

            assertThat(shortLinkStore.get(shortLink.code, shortLink.group)).isNull()

            assertThat(shortLinkStore.get(shortLink.code, shortLink.group, excludeExpired = false))
                .isNotNull
        }
    }

    @Test
    fun `Existing entry should be updated with modified parameters`() = runTest {
        with(TestClock()) {
            val shortLink = ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
            val code = shortLink.code
            val group = shortLink.group

            createDirect(shortLink)

            val newUrl = UrlFactory.random()
            val newExpiresAt = 6.minutes.fromNow().toEpochMilli()

            shortLinkStore.update(
                code,
                url = newUrl,
                group = group,
                updater = ShortLinkUser.ANONYMOUS
            )
            getDirect(code, group)!!.let { assertThat(it.url).isEqualTo(newUrl) }
            shortLinkStore.update(
                code,
                expiresAt = newExpiresAt,
                group = group,
                updater = ShortLinkUser.ANONYMOUS
            )
            getDirect(code, group)!!.let { assertThat(it.expiresAt).isEqualTo(newExpiresAt) }
        }
    }

    @Test
    fun `An exception should be thrown when trying to update a shortlink if the code does not exist`() =
        runTest {
            assertExceptionThrown(NotFoundOrNotPermittedException::class) {
                shortLinkStore.update(
                    ShortCode("Something"),
                    url = UrlFactory.random(),
                    group = ShortLinkGroup.DEFAULT,
                    updater = ShortLinkUser.ANONYMOUS
                )
            }
        }

    @Test
    fun `Updating an entry should differentiate by group`() = runTest {
        with(TestClock()) {
            val group1 = ShortLinkGroup("group1")
            val group2 = ShortLinkGroup("group2")
            val code = ShortCode("code")

            createDirect(ShortLinkFactory.build(code = code, group = group1))

            val newUrl = UrlFactory.random()
            val newExpiresAt = 6.minutes.fromNow().toEpochMilli()

            shortLinkStore.update(
                code,
                url = newUrl,
                group = group1,
                updater = ShortLinkUser.ANONYMOUS
            )
            getDirect(code, group1)!!.let { assertThat(it.url).isEqualTo(newUrl) }
            assertExceptionThrown(NotFoundOrNotPermittedException::class) {
                shortLinkStore.update(
                    code,
                    url = newUrl,
                    group = group2,
                    updater = ShortLinkUser.ANONYMOUS
                )
            }

            shortLinkStore.update(
                code,
                expiresAt = newExpiresAt,
                group = group1,
                updater = ShortLinkUser.ANONYMOUS
            )
            getDirect(code, group1)!!.let { assertThat(it.expiresAt).isEqualTo(newExpiresAt) }
            assertExceptionThrown(NotFoundOrNotPermittedException::class) {
                shortLinkStore.update(
                    code,
                    expiresAt = newExpiresAt,
                    group = group2,
                    updater = ShortLinkUser.ANONYMOUS
                )
            }
        }
    }

    @Test
    fun `An exception should be thrown when trying to update a shortlink if the owner does not match`() =
        runTest {
            val owner = ShortLinkUser("Someone")
            createDirect(ShortLinkFactory.build(owner = owner))

            assertExceptionThrown(NotFoundOrNotPermittedException::class) {
                shortLinkStore.update(
                    ShortCode("Something"),
                    url = UrlFactory.random(),
                    group = ShortLinkGroup.DEFAULT,
                    updater = ShortLinkUser.ANONYMOUS
                )
            }
            assertExceptionThrown(NotFoundOrNotPermittedException::class) {
                shortLinkStore.update(
                    ShortCode("Something"),
                    url = UrlFactory.random(),
                    group = ShortLinkGroup.DEFAULT,
                    updater = ShortLinkUser("Someone Else"),
                )
            }
        }

    @Test
    fun `Existing entry should be deleted`() = runTest {
        val shortLink = createDirect()
        val code = shortLink.code

        shortLinkStore.delete(
            code,
            group = ShortLinkGroup.DEFAULT,
            deleter = ShortLinkUser.ANONYMOUS
        )

        getDirect(shortLink.code, shortLink.group).let { assertThat(it).isNull() }
    }

    @Test
    fun `An exception should be thrown when trying to delete a shortlink if the code does not exist`() =
        runTest {
            assertExceptionThrown(NotFoundOrNotPermittedException::class) {
                shortLinkStore.delete(
                    ShortCode("Missing"),
                    group = ShortLinkGroup.DEFAULT,
                    deleter = ShortLinkUser.ANONYMOUS
                )
            }
        }

    @Test
    fun `An exception should be thrown when trying to delete a shortlink if the owner does not match`() =
        runTest {
            val owner = ShortLinkUser("Someone")
            createDirect(ShortLinkFactory.build(owner = owner))

            assertExceptionThrown(NotFoundOrNotPermittedException::class) {
                shortLinkStore.delete(
                    ShortCode("Missing"),
                    group = ShortLinkGroup.DEFAULT,
                    deleter = ShortLinkUser.ANONYMOUS
                )
            }
            assertExceptionThrown(NotFoundOrNotPermittedException::class) {
                shortLinkStore.delete(
                    ShortCode("Missing"),
                    group = ShortLinkGroup.DEFAULT,
                    deleter = ShortLinkUser("Someone Else")
                )
            }
        }

    @Test
    fun `Deletion should differentiate by group`() = runTest {
        val group1 = ShortLinkGroup("group1")
        val group2 = ShortLinkGroup("group2")
        val code = ShortCode("code")
        val shortLink = createDirect(ShortLinkFactory.build(code = code, group = group1))

        shortLinkStore.delete(code, group = group1, deleter = ShortLinkUser.ANONYMOUS)

        getDirect(shortLink.code, group1).let { assertThat(it).isNull() }

        assertExceptionThrown(NotFoundOrNotPermittedException::class) {
            shortLinkStore.delete(code, group = group2, deleter = ShortLinkUser.ANONYMOUS)
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
