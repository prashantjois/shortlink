package generator

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class NaiveShortCodeGeneratorTest {
    @Test
    fun `minimum code length should be enforced`() {
        assertThatIllegalArgumentException()
            .isThrownBy { NaiveShortCodeGenerator(0) }
            .withMessage("Code length must be >= 2")
    }

    @Test
    fun `generate() returns a code of the given length`() {
        with(NaiveShortCodeGenerator()) { assertThat(generate().code).hasSize(4) }

        with(NaiveShortCodeGenerator(length = 2)) { assertThat(generate().code).hasSize(2) }

        with(NaiveShortCodeGenerator(length = 3)) { assertThat(generate().code).hasSize(3) }
    }
}
