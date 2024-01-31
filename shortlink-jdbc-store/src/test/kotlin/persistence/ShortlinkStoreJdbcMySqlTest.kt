package persistence

import java.util.stream.Stream
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import model.ShortCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.junit.jupiter.Testcontainers
import persistence.ShortLinkStore.DuplicateShortCodeException
import persistence.testhelpers.TestDatabase
import persistence.testhelpers.TestDatabase.createShortLinkDirect
import persistence.testhelpers.TestDatabase.getShortLinkDirect
import persistence.testhelpers.TestDatabase.initShortLinkStore
import testhelpers.clock.TestClock
import testhelpers.factory.ShortLinkFactory
import testhelpers.factory.UrlFactory

@Testcontainers
class ShortlinkStoreJdbcMySqlTest {
    companion object {
        /**
         * Runs the entire set of tests in this class against a variet of databases to ensure
         * compatibility with them. See testcontainers documentation for available containers.
         */
        @JvmStatic
        fun provideDatabases(): Stream<JdbcDatabaseContainer<*>> =
            Stream.of(
                TestDatabase.mysql("8.3.0"),
                TestDatabase.mariadb("11.2"),
                TestDatabase.postgres("16.1"),
            )

        const val CONTAINER_PROVIDER = "persistence.ShortlinkStoreJdbcMySqlTest#provideDatabases"
    }

    @Nested
    @DisplayName("JdbcShortLinkStore#create")
    inner class CreateTest {
        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `shortlink should be saved`(container: JdbcDatabaseContainer<*>) = runTest {
            initShortLinkStore(container) { shortLinkStore ->
                val shortLink = ShortLinkFactory.build()
                shortLinkStore.create(shortLink)

                container.getShortLinkDirect(shortLink.code).let { assertThat(it).isNotNull }
            }
        }

        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `attempting to save shortlink with duplicate short code should throw an exception`(
            container: JdbcDatabaseContainer<*>
        ) = runTest {
            initShortLinkStore(container) { shortLinkStore ->
                val shortLink = ShortLinkFactory.build()
                var exception: DuplicateShortCodeException? = null
                try {
                    repeat(2) { shortLinkStore.create(shortLink) }
                } catch (e: DuplicateShortCodeException) {
                    exception = e
                }
                assertThat(exception).isNotNull()
            }
        }
    }

    @Nested
    @DisplayName("JdbcShortLinkStore#get")
    inner class GetTest {
        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `retrieve a previously created shortlink`(container: JdbcDatabaseContainer<*>) =
            runTest {
                initShortLinkStore(container) { shortLinkStore ->
                    with(TestClock()) {
                        val shortLink = container.createShortLinkDirect()

                        with(shortLinkStore.get(shortLink.code)!!) {
                            assertThat(url).isEqualTo(shortLink.url)
                            assertThat(code).isEqualTo(shortLink.code)
                            assertThat(createdAt).isEqualTo(shortLink.createdAt)
                            assertThat(expiresAt).isEqualTo(shortLink.expiresAt)
                        }
                    }
                }
            }

        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `should return null if a shortlink exists but is expired`(
            container: JdbcDatabaseContainer<*>
        ) = runTest {
            initShortLinkStore(container) { shortLinkStore ->
                with(TestClock()) {
                    val shortLink =
                        ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
                    container.createShortLinkDirect(shortLink)

                    advanceClockBy(5.minutes)

                    assertThat(shortLinkStore.get(shortLink.code)).isNotNull

                    advanceClockBy(1.seconds)

                    assertThat(shortLinkStore.get(shortLink.code)).isNull()
                }
            }
        }

        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `should return the shortlink if it is expired but excludeExpired is false`(
            container: JdbcDatabaseContainer<*>
        ) = runTest {
            initShortLinkStore(container) { shortLinkStore ->
                with(TestClock()) {
                    val shortLink =
                        ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())

                    container.createShortLinkDirect(shortLink)

                    advanceClockBy(6.minutes)

                    assertThat(shortLinkStore.get(shortLink.code)).isNull()

                    assertThat(shortLinkStore.get(shortLink.code, excludeExpired = false)).isNotNull
                }
            }
        }
    }

    @Nested
    @DisplayName("JdbcShortLinkStore#update")
    inner class UpdateTest {
        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `Existing entry should be updated with modified parameters`(
            container: JdbcDatabaseContainer<*>
        ) = runTest {
            initShortLinkStore(container) { shortLinkStore ->
                with(TestClock()) {
                    val shortLink =
                        ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
                    val code = shortLink.code

                    container.createShortLinkDirect(shortLink)

                    val newUrl = UrlFactory.random()
                    val newExpiresAt = 6.minutes.fromNow().toEpochMilli()

                    shortLinkStore.update(code, url = newUrl)
                    container.getShortLinkDirect(code)!!.let {
                        assertThat(it.url).isEqualTo(newUrl)
                    }
                    shortLinkStore.update(code, expiresAt = newExpiresAt)
                    container.getShortLinkDirect(code)!!.let {
                        assertThat(it.expiresAt).isEqualTo(newExpiresAt)
                    }
                }
            }
        }

        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `An exception should be thrown if the code does not exist`(
            container: JdbcDatabaseContainer<*>
        ) = runTest {
            initShortLinkStore(container) { shortLinkStore ->
                var exception: ShortLinkStore.NotFoundException? = null
                try {
                    shortLinkStore.update(ShortCode("Something"), url = UrlFactory.random())
                } catch (e: ShortLinkStore.NotFoundException) {
                    exception = e
                }

                assertThat(exception).isNotNull()
            }
        }
    }

    @Nested
    @DisplayName("JdbcShortLinkStore#delete")
    inner class DeleteTest {
        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `Existing entry should be deleted`(container: JdbcDatabaseContainer<*>) = runTest {
            initShortLinkStore(container) { shortLinkStore ->
                val shortLink = container.createShortLinkDirect()
                val code = shortLink.code

                shortLinkStore.delete(code)

                container.getShortLinkDirect(shortLink.code).let { assertThat(it).isNull() }
            }
        }

        @ParameterizedTest
        @MethodSource(CONTAINER_PROVIDER)
        fun `it should throw an exception if the short link doesn't exist`(
            container: JdbcDatabaseContainer<*>
        ) = runTest {
            initShortLinkStore(container) { shortLinkStore ->
                var exception: ShortLinkStore.NotFoundException? = null
                try {
                    shortLinkStore.delete(ShortCode("Missing"))
                } catch (e: ShortLinkStore.NotFoundException) {
                    exception = e
                }

                assertThat(exception).isNotNull
            }
        }
    }
}
