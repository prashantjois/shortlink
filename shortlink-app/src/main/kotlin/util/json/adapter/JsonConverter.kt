package shortlinkapp.util.json.adapter

import com.linecorp.armeria.common.HttpResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** Utilities to help with JSON de/serialization. */
class JsonConverter(vararg additionalAdapters: JsonAdapter<*>) {
    val moshi: Moshi

    init {
        val moshiBuilder = Moshi.Builder().add(KotlinJsonAdapterFactory())
        additionalAdapters.forEach(moshiBuilder::addLast)
        moshi = moshiBuilder.build()
    }

    /** Serializes a given object [obj] to its JSON representation */
    inline fun <reified T> toJson(obj: T): String {
        return moshi.adapter(T::class.java).toJson(obj)
    }

    /** Deserializes a JSON string into the given type [T] */
    inline fun <reified T> fromJson(json: String): T? {
        return moshi.adapter(T::class.java).fromJson(json)
    }

    /** Helper for empty JSON response `{}` */
    fun empty(): HttpResponse = HttpResponse.of("{}")

    /**
     * Serializes the given object [obj] and wraps it in an [HttpResponse] which can be used as a
     * return value for Armeria actions
     */
    inline fun <reified T> toHttpResponse(obj: T): HttpResponse =
        obj?.let { HttpResponse.of(toJson(it)) } ?: empty()
}
