package shortlinkapp.util.json.adapter

import com.squareup.moshi.*
import java.io.IOException
import java.net.URL

/** Adapter to de/serialize the URL type. */
class UrlAdapter : JsonAdapter<URL>() {
    @ToJson
    override fun toJson(writer: JsonWriter, value: URL?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.toString())
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): URL? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        val urlStr = reader.nextString()
        return try {
            URL(urlStr)
        } catch (e: Exception) {
            throw IOException("Invalid URL: $urlStr")
        }
    }
}
