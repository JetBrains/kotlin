// KIND: STANDALONE
// ASSERTIONS_MODE: always-disable

import kotlin.test.*

@Test
fun assertEnabled() {
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    assert(false)
}