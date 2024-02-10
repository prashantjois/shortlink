package api.service.shortlink

import api.service.TestWebServer
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.testhelpers.clock.TestClock
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import java.net.URL
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import shortlinkapp.api.service.shortlink.actions.CreateShortLinkAction
import shortlinkapp.api.service.shortlink.actions.DeleteShortLinkAction
import shortlinkapp.api.service.shortlink.actions.GetShortLinkAction
import shortlinkapp.api.service.shortlink.actions.UpdateShortLinkAction

class ShortLinkApiServiceTest {
    @Test
    fun `POST#create creates a new shortlink`() = runTest {
        with(server) {
            with(clock) {
                val request =
                    CreateShortLinkAction.Request(username = "user", url = "https://example.com")

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
                    UpdateShortLinkAction.UrlRequest(
                        username = "user",
                        code = shortLink.code.value,
                        url = newUrl
                    )

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
                        username = "user",
                        code = shortLink.code.value,
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

                val request = GetShortLinkAction.Request(code = shortLink.code.value)

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

                val request =
                    DeleteShortLinkAction.Request(username = "user", code = shortLink.code.value)

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
