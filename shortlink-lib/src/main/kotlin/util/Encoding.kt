package util

import java.nio.ByteBuffer
import java.util.*

object Encoding {
    fun UUID.toBase64(): String {
        val byteBuffer = ByteBuffer.wrap(ByteArray(16))
        byteBuffer.putLong(this.mostSignificantBits)
        byteBuffer.putLong(this.leastSignificantBits)
        return Base64.getUrlEncoder().encodeToString(byteBuffer.array())
    }
}
