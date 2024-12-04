// ASSERTIONS_MODE: always-disable
import kotlin.test.*

@Test
fun assertDisabled() {
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    assert(false)
}