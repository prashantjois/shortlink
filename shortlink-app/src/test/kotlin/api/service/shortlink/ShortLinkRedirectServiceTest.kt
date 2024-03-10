package api.service.shortlink

import api.service.TestWebServer
import ca.jois.shortlink.testhelpers.clock.TestClock
import ca.jois.shortlink.testhelpers.factory.ShortLinkFactory
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.HttpStatus
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ShortLinkRedirectServiceTest {
  @Test
  fun `redirect returns a 3XX response`() = runTest {
    val shortLink = ShortLinkFactory.build()
    server.shortLinkStore.create(shortLink)
    val client = WebClient.of(server.httpUri())
    val response =
      client.get("r/${shortLink.group.name}/${shortLink.code.value}").aggregate().join()
    assertThat(response.status()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
    assertThat(response.headers().get("location")).contains(shortLink.url.toString())
  }

  companion object {
    @JvmField
    @RegisterExtension
    val server = TestWebServer(TestClock())
  }
}
