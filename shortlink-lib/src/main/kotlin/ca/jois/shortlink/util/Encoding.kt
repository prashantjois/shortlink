package ca.jois.shortlink.util

import java.nio.ByteBuffer
import java.util.*

object Encoding {
  fun UUID.toBase64(): String {
    val byteBuffer = ByteBuffer.wrap(ByteArray(16))
    byteBuffer.putLong(this.mostSignificantBits)
    byteBuffer.putLong(this.leastSignificantBits)
    return Base64.getUrlEncoder().encodeToString(byteBuffer.array())
  }

  fun String.toBase64(): String = Base64.getEncoder().encodeToString(toByteArray(Charsets.UTF_8))

  fun String.fromBase64(): String = String(Base64.getDecoder().decode(this), Charsets.UTF_8)
}
