package api.service.shortlink

import api.service.TestWebServer
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.testhelpers.clock.TestClock
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import shortlinkapp.api.service.shortlink.actions.CreateShortLinkAction
import shortlinkapp.api.service.shortlink.actions.DeleteShortLinkAction
import shortlinkapp.api.service.shortlink.actions.GetShortLinkAction
import shortlinkapp.api.service.shortlink.actions.ListShortLinksAction
import shortlinkapp.api.service.shortlink.actions.UpdateShortLinkAction
import java.net.URL
import kotlin.time.Duration.Companion.minutes

class ShortLinkApiServiceTest {
  @BeforeEach
  fun setUp() {
    server.shortLinkStore.clear()
  }

  @Test
  fun `GET#list retrieves all shortlinks`() = runTest {
    with(server) {
      val group = ShortLinkGroup.DEFAULT
      val owner = ShortLinkUser("user")
      val shortLinks =
        (1..5).map { server.shortLinkStore.create(ShortLinkFactory.build(owner = owner)) }
      (1..5).map {
        server.shortLinkStore.create(
          ShortLinkFactory.build(owner = ShortLinkUser.ANONYMOUS),
        )
      }

      val request = ListShortLinksAction.Request(group = group.name, owner = owner.identifier)
      get<ListShortLinksAction.Request, ListShortLinksAction.Response>(
        request,
        "/listByOwner",
      ) {
        assertThat(it!!.entries).containsExactlyInAnyOrderElementsOf(shortLinks)
      }
    }
  }

  @Test
  fun `POST#create creates a new shortlink`() = runTest {
    with(server) {
      with(clock) {
        val request =
          CreateShortLinkAction.Request(
            creator = "user",
            group = "group",
            url = "https://example.com",
          )

        post<CreateShortLinkAction.Request, ShortLink>(request, "/create") {
          assertThat(it!!).isNotNull
          assertThat(server.shortLinkStore.get(it.code, it.group)).isEqualTo(it)
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
            group = shortLink.group.name,
            code = shortLink.code.value,
            url = newUrl,
          )

        put<UpdateShortLinkAction.UrlRequest, ShortLink>(request, "/update/url") {
          assertThat(it!!.url).isEqualTo(URL(newUrl))
          assertThat(server.shortLinkStore.get(it.code, it.group)!!.url)
            .isEqualTo(URL(newUrl))
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
            group = shortLink.group.name,
            code = shortLink.code.value,
            expiresAt = newExpiry,
          )

        put<UpdateShortLinkAction.ExpiryRequest, ShortLink>(request, "/update/expiry") {
          assertThat(it!!.expiresAt).isEqualTo(newExpiry)
          assertThat(server.shortLinkStore.get(it.code, it.group)!!.expiresAt)
            .isEqualTo(newExpiry)
        }
      }
    }
  }

  @Test
  fun `GET#get retrieves an existing shortlink`() = runTest {
    with(server) {
      with(clock) {
        val shortLink = server.shortLinkStore.create(ShortLinkFactory.build())

        val request =
          GetShortLinkAction.Request(
            group = shortLink.group.name,
            code = shortLink.code.value,
          )

        get<GetShortLinkAction.Request, ShortLink>(request, "/get") {
          assertThat(server.shortLinkStore.get(it!!.code, it.group)!!)
            .isEqualTo(shortLink)
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
          DeleteShortLinkAction.Request(
            username = "user",
            group = shortLink.group.name,
            code = shortLink.code.value,
          )

        delete<DeleteShortLinkAction.Request, Unit>(request, "/delete")
        assertThat(server.shortLinkStore.get(shortLink.code, shortLink.group)).isNull()
      }
    }
  }

  companion object {
    val clock = TestClock()

    @JvmField
    @RegisterExtension
    val server = TestWebServer(clock)
  }
}
