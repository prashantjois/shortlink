package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.persistence.ShortLinkStore.DuplicateShortCodeException
import ca.jois.shortlink.testhelpers.clock.TestClock
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import ca.jois.shortlink.testhelpers.factory.UrlFactory
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ShortLinkStoreInMemoryTest {
    private val shortLinkStoreInMemory = ShortLinkStoreInMemory()

    @Nested
    @DisplayName("ShortLinkStoreInMemory#create")
    inner class CreateTest {
        @Test
        fun `shortlink should be saved`() = runTest {
            with(TestClock()) {
                val shortLink = ShortLinkFactory.build()
                shortLinkStoreInMemory.create(shortLink)

                assertThat(shortLinkStoreInMemory.get(shortLink.code)).isNotNull
            }
        }

        @Test
        fun `attempting to save shortlink with duplicate short code should throw an exception`() =
            runTest {
                val shortLink = ShortLinkFactory.build()
                var exception: DuplicateShortCodeException? = null
                try {
                    repeat(2) { shortLinkStoreInMemory.create(shortLink) }
                } catch (e: DuplicateShortCodeException) {
                    exception = e
                }
                assertThat(exception).isNotNull()
            }
    }

    @Nested
    @DisplayName("ShortLinkStoreInMemory#get")
    inner class GetTest {
        @Test
        fun `retrieve a previously created shortlink`() = runTest {
            with(TestClock()) {
                val shortLink = ShortLinkFactory.build()

                shortLinkStoreInMemory.create(shortLink)

                with(shortLinkStoreInMemory.get(shortLink.code)!!) {
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

                shortLinkStoreInMemory.create(shortLink)

                advanceClockBy(5.minutes)

                assertThat(shortLinkStoreInMemory.get(shortLink.code)).isNotNull

                advanceClockBy(1.seconds)

                assertThat(shortLinkStoreInMemory.get(shortLink.code)).isNull()
            }
        }

        @Test
        fun `should return the shortlink if it is expired but excludeExpired is false`() = runTest {
            with(TestClock()) {
                val shortLink =
                    ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())

                shortLinkStoreInMemory.create(shortLink)

                advanceClockBy(6.minutes)

                assertThat(shortLinkStoreInMemory.get(shortLink.code)).isNull()

                assertThat(shortLinkStoreInMemory.get(shortLink.code, excludeExpired = false))
                    .isNotNull
            }
        }
    }

    @Nested
    @DisplayName("ShortLinkStoreInMemory#update")
    inner class UpdateTest {
        @Test
        fun `Existing entry should be updated with modified parameters`() = runTest {
            with(TestClock()) {
                val shortLink =
                    ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
                val code = shortLink.code

                shortLinkStoreInMemory.create(shortLink)

                shortLinkStoreInMemory.get(code).let {
                    assertThat(it).isNotNull
                    assertThat(it).isEqualTo(shortLink)
                }

                val newUrl = UrlFactory.random()
                val newExpiresAt = 6.minutes.fromNow().toEpochMilli()

                shortLinkStoreInMemory.update(code, url = newUrl)
                shortLinkStoreInMemory.get(code)!!.let { assertThat(it.url).isEqualTo(newUrl) }
                shortLinkStoreInMemory.update(code, expiresAt = newExpiresAt)
                shortLinkStoreInMemory.get(code)!!.let {
                    assertThat(it.expiresAt).isEqualTo(newExpiresAt)
                }
            }
        }

        @Test
        fun `An exception should be thrown if the code does not exist`() = runTest {
            var exception: ShortLinkStore.NotFoundException? = null
            try {
                shortLinkStoreInMemory.update(ShortCode("Something"), url = UrlFactory.random())
            } catch (e: ShortLinkStore.NotFoundException) {
                exception = e
            }

            assertThat(exception).isNotNull()
        }
    }

    @Nested
    @DisplayName("ShortLinkStoreInMemory#delete")
    inner class DeleteTest {
        @Test
        fun `Existing entry should be deleted`() = runTest {
            with(TestClock()) {
                val shortLink = ShortLinkFactory.build()
                val code = shortLink.code

                shortLinkStoreInMemory.create(shortLink)

                shortLinkStoreInMemory.get(code).let {
                    assertThat(it).isNotNull
                    assertThat(it).isEqualTo(shortLink)
                }

                shortLinkStoreInMemory.delete(code)

                assertThat(shortLinkStoreInMemory.get(code)).isNull()
            }
        }

        @Test
        fun `it should throw an exception if the short link doesn't exist`() = runTest {
            var exception: ShortLinkStore.NotFoundException? = null
            try {
                shortLinkStoreInMemory.delete(ShortCode("Missing"))
            } catch (e: ShortLinkStore.NotFoundException) {
                exception = e
            }

            assertThat(exception).isNotNull
        }
    }
}
