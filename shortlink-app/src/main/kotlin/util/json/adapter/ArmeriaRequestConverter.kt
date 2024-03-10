package shortlinkapp.util.json.adapter

import com.linecorp.armeria.common.AggregatedHttpRequest
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.RequestConverterFunction
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.ParameterizedType

/** A utility class that converts a JSON string to its deserialized form for Armeria requests */
object ArmeriaRequestConverter : RequestConverterFunction {
  private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  override fun convertRequest(
    ctx: ServiceRequestContext,
    request: AggregatedHttpRequest,
    expectedResultType: Class<*>,
    expectedParameterizedResultType: ParameterizedType?
  ) = with(moshi.adapter(expectedResultType)) { fromJson(request.contentUtf8()) }
}
