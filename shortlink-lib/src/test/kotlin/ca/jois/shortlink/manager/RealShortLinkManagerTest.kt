package ca.jois.shortlink.manager

import ca.jois.shortlink.generator.NaiveShortCodeGenerator
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.testhelpers.ShortLinkStoreFake
import ca.jois.shortlink.testhelpers.clock.TestClock
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import ca.jois.shortlink.testhelpers.factory.UrlFactory
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RealShortLinkManagerTest {
    @Nested
    @DisplayName("RealShortLinkManager#create")
    inner class CreateTest {
        @Test
        fun `ShortLink is created with the provided params`() = runTest {
            with(TestClock()) {
                val shortLinkStore = ShortLinkStoreFake()
                val realShortLinkManager =
                    RealShortLinkManager(
                        shortCodeGenerator = NaiveShortCodeGenerator(),
                        shortLinkStore = shortLinkStore
                    )
                val url = UrlFactory.random()
                val shortLink =
                    realShortLinkManager.create(
                        creator = ShortLinkUser("user"),
                        url = url,
                        expiresAt = 5.minutes.fromNow().toEpochMilli()
                    )
                val createdShortLink = shortLinkStore.get(shortLink.code)
                assertThat(createdShortLink).isEqualTo(shortLink)
            }
        }
    }

    @Nested
    @DisplayName("RealShortLinkManager#get")
    inner class GetTest {
        private val clock = TestClock()
        private val shortLinkStore = ShortLinkStoreFake()
        private lateinit var realShortLinkManager: RealShortLinkManager

        @BeforeEach
        fun setup() {
            with(clock) {
                realShortLinkManager =
                    RealShortLinkManager(
                        shortCodeGenerator = NaiveShortCodeGenerator(),
                        shortLinkStore = shortLinkStore
                    )
            }
        }

        @Test
        fun `it should retrieve an existing ShortLink`() = runTest {
            val shortLink = shortLinkStore.create(ShortLinkFactory.build())
            realShortLinkManager.get(shortLink.code).let {
                assertThat(it).isNotNull
                assertThat(it).isEqualTo(shortLink)
            }
        }

        @Test
        fun `it should return null if the code does not exist`() {
            assertThat(realShortLinkManager.get(ShortCode("random"))).isNull()
        }

        @Test
        fun `it should return null if the code is expired`() = runTest {
            with(clock) {
                val shortLink =
                    ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli()).let {
                        shortLinkStore.create(it)
                    }

                realShortLinkManager.get(shortLink.code).let {
                    assertThat(it).isNotNull
                    assertThat(it).isEqualTo(shortLink)
                }

                advanceClockBy(6.minutes)

                assertThat(realShortLinkManager.get(shortLink.code)).isNull()
            }
        }
    }

    @Nested
    @DisplayName("RealShortLinkManager#update")
    inner class UpdateUrlTest {
        private val clock = TestClock()
        private val shortLinkStore = ShortLinkStoreFake()
        private lateinit var realShortLinkManager: RealShortLinkManager

        @BeforeEach
        fun setup() {
            with(clock) {
                realShortLinkManager =
                    RealShortLinkManager(
                        shortCodeGenerator = NaiveShortCodeGenerator(),
                        shortLinkStore = shortLinkStore
                    )
            }
        }

        @Test
        fun `it should update an existing short link`() = runTest {
            with(clock) {
                val shortLink =
                    ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
                val code = shortLink.code
                shortLinkStore.create(shortLink)

                val newUrl = UrlFactory.random()

                realShortLinkManager.update(null, code, url = newUrl)

                shortLinkStore.get(code)!!.let { assertThat(it.url).isEqualTo(newUrl) }

                val newExpiry = 6.minutes.fromNow().toEpochMilli()
                realShortLinkManager.update(null, code, expiresAt = newExpiry).let {
                    assertThat(it.expiresAt).isEqualTo(newExpiry)
                }
            }
        }
    }

    @Nested
    @DisplayName("RealShortLinkManager#delete")
    inner class DeleteTest {
        private val clock = TestClock()
        private val shortLinkStore = ShortLinkStoreFake()
        private lateinit var realShortLinkManager: RealShortLinkManager

        @BeforeEach
        fun setup() {
            with(clock) {
                realShortLinkManager =
                    RealShortLinkManager(
                        shortCodeGenerator = NaiveShortCodeGenerator(),
                        shortLinkStore = shortLinkStore
                    )
            }
        }

        @Test
        fun `it should delete an existing entry`() = runTest {
            with(clock) {
                val shortLink =
                    ShortLinkFactory.build(expiresAt = 5.minutes.fromNow().toEpochMilli())
                val code = shortLink.code
                shortLinkStore.create(shortLink)

                realShortLinkManager.delete(null, code)

                assertThat(shortLinkStore.get(code)).isNull()
            }
        }
    }
}
