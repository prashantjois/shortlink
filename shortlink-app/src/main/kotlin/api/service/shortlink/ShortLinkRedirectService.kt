package shortlinkapp.api.service.shortlink

import ca.jois.shortlink.manager.RealShortLinkManager
import ca.jois.shortlink.model.ShortCode
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param

class ShortLinkRedirectService(private val shortLinkManager: RealShortLinkManager) {
    @Get("/{code}")
    fun redirect(@Param("code") code: String): HttpResponse {
        val shortLink =
            shortLinkManager.get(ShortCode(code))
                ?: throw RuntimeException("Short link with code $code not found")

        return HttpResponse.ofRedirect(shortLink.url.toString())
    }
}
