// ISSUE: KT-89739

import lombok.Builder
import lombok.Singular
import kotlin.test.assertTrue

@Builder
class Mutable {
    @Singular var integers: List<Int> = emptyList()

    @Singular var strings: List<String> = emptyList()

    @Singular var booleans: List<Boolean> = emptyList()

    companion object {
        fun create(): Mutable {
            return builder()
                .integer(1).integer(2)
                .string("O").string("K")
                .boolean(false).boolean(true)
                .build()
        }
    }
}

fun box(): String {
    val m = Mutable.create()
    assertTrue(m.integers[0] == 1 && m.integers[1] == 2, "Fail integers: ${m.integers}")
    assertTrue(!m.booleans[0] && m.booleans[1], "Fail booleans: ${m.booleans}")
    return m.strings[0] + m.strings[1]
}
