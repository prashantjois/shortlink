package api.service

import ca.jois.shortlink.generator.NaiveShortCodeGenerator
import ca.jois.shortlink.generator.ShortCodeGenerator
import ca.jois.shortlink.manager.RealShortLinkManager
import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.persistence.ShortLinkStore
import ca.jois.shortlink.persistence.ShortLinkStoreInMemory
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.QueryParams
import com.linecorp.armeria.common.RequestHeaders
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.testing.junit5.server.ServerExtension
import java.time.Clock
import java.util.*
import shortlinkapp.api.service.shortlink.ShortLinkApiService
import shortlinkapp.api.service.shortlink.ShortLinkRedirectService
import shortlinkapp.api.service.shortlink.actions.CreateShortLinkAction
import shortlinkapp.api.service.shortlink.actions.DeleteShortLinkAction
import shortlinkapp.api.service.shortlink.actions.GetShortLinkAction
import shortlinkapp.api.service.shortlink.actions.UpdateShortLinkAction
import shortlinkapp.util.json.adapter.JsonConverter
import shortlinkapp.util.json.adapter.UrlAdapter

/**
 * An Armeria server for testing purposes.
 *
 * Use in your test like so:
 * ```
 *  class MyTest {
 *     companion object {
 *         val clock = TestClock()
 *
 *         @JvmField @RegisterExtension val server = TestWebServer(clock)
 *     }
 *
 *     @Test
 *     fun `POST#myMethod does things`() {
 *         with(server) {
 *             val request = MyRequest(param1 = "param")
 *
 *             post<MyRequest, MyResponse>(request, "/create") {
 *                 assertThat(it).isNotNull()
 *             }
 *         }
 *     }
 *  }
 *  ```
 */
class TestWebServer(clock: Clock) : ServerExtension() {
    val shortLinkStore: ShortLinkStore
    val shortLinkManager: ShortLinkManager
    val shortCodeGenerator: ShortCodeGenerator
    val createShortLinkAction: CreateShortLinkAction
    val getShortLinkAction: GetShortLinkAction
    val updateShortLinkAction: UpdateShortLinkAction
    val deleteShortLinkAction: DeleteShortLinkAction
    val shortLinkRedirectService: ShortLinkRedirectService
    val shortLinkApiService: ShortLinkApiService
    val converter = JsonConverter(UrlAdapter())

    init {
        with(clock) {
            shortCodeGenerator = NaiveShortCodeGenerator()
            shortLinkStore = ShortLinkStoreInMemory()
            shortLinkManager =
                RealShortLinkManager(
                    shortCodeGenerator = shortCodeGenerator,
                    shortLinkStore = shortLinkStore
                )
            createShortLinkAction = CreateShortLinkAction(shortLinkManager)
            getShortLinkAction = GetShortLinkAction(shortLinkManager)
            updateShortLinkAction = UpdateShortLinkAction(shortLinkManager)
            deleteShortLinkAction = DeleteShortLinkAction(shortLinkManager)
            shortLinkApiService =
                ShortLinkApiService(
                    createShortLinkAction,
                    getShortLinkAction,
                    updateShortLinkAction,
                    deleteShortLinkAction,
                )
            shortLinkRedirectService = ShortLinkRedirectService(shortLinkManager)
        }
    }

    override fun configure(sb: ServerBuilder) {
        sb.annotatedService(shortLinkApiService).annotatedService("/r", shortLinkRedirectService)
    }

    inline fun <reified REQ_TYPE, reified RESP_TYPE> post(
        request: REQ_TYPE,
        path: String,
        params: QueryParams? = null,
        handle: (RESP_TYPE?) -> Unit = {}
    ) = execute<REQ_TYPE, RESP_TYPE>(HttpMethod.POST, request, path, params, handle)

    inline fun <reified REQ_TYPE, reified RESP_TYPE> get(
        request: REQ_TYPE,
        path: String,
        params: QueryParams? = null,
        handle: (RESP_TYPE?) -> Unit = {}
    ) = execute<REQ_TYPE, RESP_TYPE>(HttpMethod.GET, request, path, params, handle)

    inline fun <reified REQ_TYPE, reified RESP_TYPE> put(
        request: REQ_TYPE,
        path: String,
        params: QueryParams? = null,
        handle: (RESP_TYPE?) -> Unit = {}
    ) = execute<REQ_TYPE, RESP_TYPE>(HttpMethod.PUT, request, path, params, handle)

    inline fun <reified REQ_TYPE, reified RESP_TYPE> delete(
        request: REQ_TYPE,
        path: String,
        params: QueryParams? = null,
        handle: (RESP_TYPE?) -> Unit = {}
    ) = execute<REQ_TYPE, RESP_TYPE>(HttpMethod.DELETE, request, path, params, handle)

    inline fun <reified REQ_TYPE, reified RESP_TYPE> execute(
        method: HttpMethod,
        request: REQ_TYPE,
        path: String,
        params: QueryParams? = null,
        handle: (RESP_TYPE?) -> Unit
    ) {
        val client = WebClient.of(httpUri())
        val content = converter.toJson(request)
        val response =
            client
                .execute(RequestHeaders.of(method, addQueryParams(path, params)), content)
                .aggregate()
                .join()
        when (RESP_TYPE::class) {
            Unit::class -> converter.empty()
            else -> handle(converter.fromJson<RESP_TYPE>(response.contentUtf8()))
        }
    }

    fun addQueryParams(path: String, params: QueryParams?): String {
        Objects.requireNonNull(path, "path")
        if (params == null || params.isEmpty) {
            return path
        }

        val appendedPath = StringBuilder(path)
        if (path.indexOf('?') == -1) {
            appendedPath.append('?')
        } else {
            appendedPath.append('&')
        }
        return params.appendQueryString(appendedPath).toString()
    }
}
