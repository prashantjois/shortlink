package shortlinkapp.api.service.shortlink

import ca.jois.shortlink.manager.RealShortLinkManager
import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLinkGroup
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param

class ShortLinkRedirectService(private val shortLinkManager: RealShortLinkManager) {
    @Get("/{group}/{code}")
    fun redirect(@Param("group") group: String, @Param("code") code: String): HttpResponse {
        val shortLink =
            shortLinkManager.get(ShortCode(code), ShortLinkGroup(group))
                ?: throw RuntimeException("Short link with code $code not found")

        return HttpResponse.ofRedirect(shortLink.url.toString())
    }
}
