// KIND: STANDALONE
// ASSERTIONS_MODE: always-enable

import kotlin.test.*

@Test
fun assertEnabled() {
    assertFailsWith<AssertionError> {
        @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
        assert(false)
    }
}