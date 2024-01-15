package generator

import java.util.UUID
import model.ShortCode
import util.Encoding.toBase64

/**
 * A short code generator that picks a code from a space of codes of a certain size and assumes the
 * space is sufficiently large such that collisions won't occur (hence: "naive"). This is not true
 * in the general case of course, but in many (most?) use-cases this is probably a reasonable
 * assumption when coupled with retries.
 *
 * Here are the chances of collision of various code lengths using base 64 encoding up to a code
 * length of 7 (which typically what we consider sufficiently large to be inexhaustible for
 * practical purposes):
 *
 * ```
 * |-------------|:-----------------:|:---------------------|:------------------|:-----------------------------------------|
 * | Code Length |   Possibilities   | Half-life (1000 RPS) | Half-life (1 RPS) | Approx. Chance of Collision at half-life |
 * |-------------|:-----------------:|:---------------------|:------------------|:-----------------------------------------|
 * | 2           |       4096        | 2 seconds            | 30 minutes        | 1 in 2000                                |
 * | 3           |      262,144      | 2 minutes            | 1 days            | 1 in 100,000                             |
 * | 4           |    16,777,216     | 2 hours              | 3 months          | 1 in 8 million                           |
 * | 5           |   1,073,741,824   | 6 days               | 17 years          | 1 in 500 million                         |
 * | 6           |  68,719,476,736   | 1 years              | 1000 years        | 1 in 34 billion                          |
 * | 7           | 4,398,046,511,104 | 70 years             | 70,000 years      | 1 in 2 trillion                          |
 * |-------------|:-----------------:|:---------------------|:------------------|:-----------------------------------------|
 * ```
 */
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
        private const val DEFAULT_CODE_LENGTH = 4

        // Arbitrarily enforce a min length of 2 (4,096 codes) as anything less is not practical
        private const val MIN_CODE_LENGTH = 2
    }
}
