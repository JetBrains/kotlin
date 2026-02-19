// IGNORE_NATIVE: optimizationMode=OPT
import kotlin.test.*

@Test
fun assertEnabled() {
    assertFailsWith<AssertionError> {
        @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
        assert(false)
    }
}