package api.service.shortlink

import api.service.TestWebServer
import java.net.URL
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import model.ShortLink
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import shortlinkapp.api.service.shortlink.actions.CreateShortLinkAction
import shortlinkapp.api.service.shortlink.actions.DeleteShortLinkAction
import shortlinkapp.api.service.shortlink.actions.GetShortLinkAction
import shortlinkapp.api.service.shortlink.actions.UpdateShortLinkAction
import testhelpers.clock.TestClock
import testhelpers.factory.ShortLinkFactory

class ShortLinkServiceTest {
    @Test
    fun `POST#create creates a new shortlink`() = runTest {
        with(server) {
            with(clock) {
                val request = CreateShortLinkAction.Request(url = "https://example.com")

                post<CreateShortLinkAction.Request, ShortLink>(request, "/create") {
                    assertThat(it!!).isNotNull
                    assertThat(server.shortLinkStore.get(it.code)).isEqualTo(it)
                }
            }
        }
    }

    @Test
    fun `PUT#update(url) updates an existing shortlink`() = runTest {
        with(server) {
            with(clock) {
                val shortLink = server.shortLinkStore.create(ShortLinkFactory.build())

                val newUrl = "https://example.com"

                val request =
                    UpdateShortLinkAction.UrlRequest(code = shortLink.code.code, url = newUrl)

                put<UpdateShortLinkAction.UrlRequest, ShortLink>(request, "/update/url") {
                    assertThat(it!!.url).isEqualTo(URL(newUrl))
                    assertThat(server.shortLinkStore.get(it.code)!!.url).isEqualTo(URL(newUrl))
                }
            }
        }
    }

    @Test
    fun `PUT#update(expiry) updates an existing shortlink`() = runTest {
        with(server) {
            with(clock) {
                val shortLink = server.shortLinkStore.create(ShortLinkFactory.build())

                val newExpiry = 5.minutes.fromNow().toEpochMilli()

                val request =
                    UpdateShortLinkAction.ExpiryRequest(
                        code = shortLink.code.code,
                        expiresAt = newExpiry
                    )

                put<UpdateShortLinkAction.ExpiryRequest, ShortLink>(request, "/update/expiry") {
                    assertThat(it!!.expiresAt).isEqualTo(newExpiry)
                    assertThat(server.shortLinkStore.get(it.code)!!.expiresAt).isEqualTo(newExpiry)
                }
            }
        }
    }

    @Test
    fun `GET#get retrieves an existing shortlink`() = runTest {
        with(server) {
            with(clock) {
                val shortLink = server.shortLinkStore.create(ShortLinkFactory.build())

                val request = GetShortLinkAction.Request(code = shortLink.code.code)

                get<GetShortLinkAction.Request, ShortLink>(request, "/get") {
                    assertThat(server.shortLinkStore.get(it!!.code)!!).isEqualTo(shortLink)
                }
            }
        }
    }

    @Test
    fun `DELETE#delete removes an existing shortlink`() = runTest {
        with(server) {
            with(clock) {
                val shortLink = server.shortLinkStore.create(ShortLinkFactory.build())

                val request = DeleteShortLinkAction.Request(code = shortLink.code.code)

                delete<DeleteShortLinkAction.Request, Unit>(request, "/delete")
                assertThat(server.shortLinkStore.get(shortLink.code)).isNull()
            }
        }
    }

    companion object {
        val clock = TestClock()

        @JvmField @RegisterExtension val server = TestWebServer(clock)
    }
}
