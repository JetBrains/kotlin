// KIND: STANDALONE_NO_TR
// ASSERTIONS_MODE: always-disable

import kotlin.test.*

fun main() {
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    assert(false)
}