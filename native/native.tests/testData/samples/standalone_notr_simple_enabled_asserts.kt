// KIND: STANDALONE_NO_TR
// ASSERTIONS_MODE: always-enable

import kotlin.test.*

fun main() {
    assertFailsWith<AssertionError> {
        @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
        assert(false)
    }
}