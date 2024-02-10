package shortlinkapp.api.service.shortlink

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.annotation.Delete
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Post
import com.linecorp.armeria.server.annotation.Put
import com.linecorp.armeria.server.annotation.RequestConverter
import shortlinkapp.api.service.shortlink.actions.CreateShortLinkAction
import shortlinkapp.api.service.shortlink.actions.DeleteShortLinkAction
import shortlinkapp.api.service.shortlink.actions.GetShortLinkAction
import shortlinkapp.api.service.shortlink.actions.UpdateShortLinkAction
import shortlinkapp.util.json.adapter.ArmeriaRequestConverter
import shortlinkapp.util.json.adapter.JsonConverter
import shortlinkapp.util.json.adapter.UrlAdapter

class ShortLinkApiService(
    private val createShortLinkAction: CreateShortLinkAction,
    private val getShortLinkAction: GetShortLinkAction,
    private val updateShortLinkAction: UpdateShortLinkAction,
    private val deleteShortLinkAction: DeleteShortLinkAction,
) {
    private val responseConverter = JsonConverter(UrlAdapter())

    @Post("/create")
    @RequestConverter(ArmeriaRequestConverter::class)
    fun create(request: CreateShortLinkAction.Request) =
        responseConverter.toHttpResponse(createShortLinkAction.handle(request))

    @Get("/get")
    @RequestConverter(ArmeriaRequestConverter::class)
    fun get(request: GetShortLinkAction.Request) =
        responseConverter.toHttpResponse(getShortLinkAction.handle(request))

    @Put("/update/url")
    @RequestConverter(ArmeriaRequestConverter::class)
    fun update(request: UpdateShortLinkAction.UrlRequest) =
        responseConverter.toHttpResponse(updateShortLinkAction.handle(request))

    @Put("/update/expiry")
    @RequestConverter(ArmeriaRequestConverter::class)
    fun update(request: UpdateShortLinkAction.ExpiryRequest) =
        responseConverter.toHttpResponse(updateShortLinkAction.handle(request))

    @Delete("/delete")
    @RequestConverter(ArmeriaRequestConverter::class)
    fun delete(request: DeleteShortLinkAction.Request): HttpResponse {
        deleteShortLinkAction.handle(request)
        return responseConverter.empty()
    }
}
