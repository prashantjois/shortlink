package generator

import java.util.UUID
import model.ShortCode
import util.Encoding.toBase64

class NaiveShortCodeGenerator(private val length: Int = DEFAULT_CODE_LENGTH) : ShortCodeGenerator {
    init {
        require(length >= MIN_CODE_LENGTH) { "Code length must be >= $MIN_CODE_LENGTH" }
    }

    override fun generate(): ShortCode {
        val uuid = UUID.randomUUID()
        val base64Encoded = uuid.toBase64()
        val shortCode = ShortCode(base64Encoded.take(length))
        return shortCode
    }

    companion object {
        // For a Base 64 value, code length of 4 generates 16,777,216 possibilities,
        // which gives a 0.0000059605% chance of collision
        private const val DEFAULT_CODE_LENGTH = 4

        // Arbitrarily enforce a min length of 2 (4,096 codes) as anything less is not practical
        private const val MIN_CODE_LENGTH = 2
    }
}
