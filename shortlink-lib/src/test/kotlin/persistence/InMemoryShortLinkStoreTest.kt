package persistence

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import model.ShortCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import persistence.ShortLinkStore.DuplicateShortCodeException
import testhelpers.clock.TestClock
import testhelpers.factory.ShortLinkFactory
import testhelpers.factory.UrlFactory

class InMemoryShortLinkStoreTest {
    private val inMemoryShortLinkStore = InMemoryShortLinkStore()

    @Nested
    @DisplayName("InMemoryShortLinkStore#create")
    inner class CreateTest {
        @Test
        fun `shortlink should be saved`() = runTest {
            with(TestClock()) {
                val shortLink = ShortLinkFactory.build()
                inMemoryShortLinkStore.create(shortLink)

                assertThat(inMemoryShortLinkStore.get(shortLink.code)).isNotNull
            }
        }

        @Test
        fun `attempting to save shortlink with duplicate short code should throw an exception`() =
            runTest {
                val shortLink = ShortLinkFactory.build()
                var exception: DuplicateShortCodeException? = null
                try {
                    repeat(2) { inMemoryShortLinkStore.create(shortLink) }
                } catch (e: DuplicateShortCodeException) {
                    exception = e
                }
                assertThat(exception).isNotNull()
            }
    }

    @Nested
    @DisplayName("InMemoryShortLinkStore#get")
    inner class GetTest {
        @Test
        fun `retrieve a previously created shortlink`() = runTest {
            with(TestClock()) {
                val shortLink = ShortLinkFactory.build()

                inMemoryShortLinkStore.create(shortLink)

                with(inMemoryShortLinkStore.get(shortLink.code)!!) {
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
                val shortLink =
                    ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())

                inMemoryShortLinkStore.create(shortLink)

                advanceClockBy(5.minutes)

                assertThat(inMemoryShortLinkStore.get(shortLink.code)).isNotNull

                advanceClockBy(1.seconds)

                assertThat(inMemoryShortLinkStore.get(shortLink.code)).isNull()
            }
        }

        @Test
        fun `should return the shortlink if it is expired but excludeExpired is false`() = runTest {
            with(TestClock()) {
                val shortLink =
                    ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())

                inMemoryShortLinkStore.create(shortLink)

                advanceClockBy(6.minutes)

                assertThat(inMemoryShortLinkStore.get(shortLink.code)).isNull()

                assertThat(inMemoryShortLinkStore.get(shortLink.code, excludeExpired = false))
                    .isNotNull
            }
        }
    }

    @Nested
    @DisplayName("InMemoryShortLinkStore#update")
    inner class UpdateTest {
        @Test
        fun `Existing entry should be updated with modified parameters`() = runTest {
            with(TestClock()) {
                val shortLink =
                    ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
                val code = shortLink.code

                inMemoryShortLinkStore.create(shortLink)

                inMemoryShortLinkStore.get(code).let {
                    assertThat(it).isNotNull
                    assertThat(it).isEqualTo(shortLink)
                }

                val newUrl = UrlFactory.random()
                val newCreatedAt = 1.minutes.fromNow().toEpochMilli()
                val newExpiresAt = 6.minutes.fromNow().toEpochMilli()

                // This is only what the method returns, not necessarily what was persisted
                inMemoryShortLinkStore
                    .update(code) {
                        it.copy(url = newUrl, createdAt = newCreatedAt, expiresAt = newExpiresAt)
                    }
                    .let {
                        assertThat(it.code).isEqualTo(code)
                        assertThat(it.url).isEqualTo(newUrl)
                        assertThat(it.createdAt).isEqualTo(newCreatedAt)
                        assertThat(it.expiresAt).isEqualTo(newExpiresAt)
                    }

                // This is what was actually persisted, tested separately from what the method
                // returns
                inMemoryShortLinkStore.get(code)!!.let {
                    assertThat(it.code).isEqualTo(code)
                    assertThat(it.url).isEqualTo(newUrl)
                    assertThat(it.createdAt).isEqualTo(newCreatedAt)
                    assertThat(it.expiresAt).isEqualTo(newExpiresAt)
                }
            }
        }

        @Test
        fun `An exception should be thrown if the code does not exist`() = runTest {
            var exception: ShortLinkStore.NotFoundException? = null
            try {
                inMemoryShortLinkStore.update(ShortCode("Something")) { it }
            } catch (e: ShortLinkStore.NotFoundException) {
                exception = e
            }

            assertThat(exception).isNotNull()
        }

        @Test
        fun `An exception should be thrown if the code is changed`() = runTest {
            with(TestClock()) {
                val shortLink = ShortLinkFactory.build()
                val code = shortLink.code

                inMemoryShortLinkStore.create(shortLink)

                inMemoryShortLinkStore.get(code).let {
                    assertThat(it).isNotNull
                    assertThat(it).isEqualTo(shortLink)
                }

                var exception: ShortLinkStore.IllegalUpdateException? = null
                try {
                    inMemoryShortLinkStore.update(code) { it.copy(code = ShortCode("New")) }
                } catch (e: ShortLinkStore.IllegalUpdateException) {
                    exception = e
                }

                assertThat(exception).isNotNull()
            }
        }
    }

    @Nested
    @DisplayName("InMemoryShortLinkStore#delete")
    inner class DeleteTest {
        @Test
        fun `Existing entry should be deleted`() = runTest {
            with(TestClock()) {
                val shortLink = ShortLinkFactory.build()
                val code = shortLink.code

                inMemoryShortLinkStore.create(shortLink)

                inMemoryShortLinkStore.get(code).let {
                    assertThat(it).isNotNull
                    assertThat(it).isEqualTo(shortLink)
                }

                inMemoryShortLinkStore.delete(code)

                assertThat(inMemoryShortLinkStore.get(code)).isNull()
            }
        }

        @Test
        fun `it should throw an exception if the short link doesn't exist`() = runTest {
            var exception: ShortLinkStore.NotFoundException? = null
            try {
                inMemoryShortLinkStore.delete(ShortCode("Missing"))
            } catch (e: ShortLinkStore.NotFoundException) {
                exception = e
            }

            assertThat(exception).isNotNull
        }
    }
}
