package util

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*

object Encoding {
  fun String.md5(): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(this.toByteArray())
  }

  fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

  fun Long.toBase64(): String {
    val bytes = ByteBuffer.allocate(8).putLong(this).array()
    return Base64.getEncoder().encodeToString(bytes)
  }

  fun UUID.toBase64(): String {
    val byteBuffer = ByteBuffer.wrap(ByteArray(16))
    byteBuffer.putLong(this.mostSignificantBits)
    byteBuffer.putLong(this.leastSignificantBits)
    return Base64.getEncoder().encodeToString(byteBuffer.array())
  }
}