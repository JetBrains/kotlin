// KIND: STANDALONE_NO_TR
// IGNORE_NATIVE: optimizationMode=OPT

import kotlin.test.*

fun main() {
    assertFailsWith<AssertionError> {
        @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
        assert(false)
    }
}