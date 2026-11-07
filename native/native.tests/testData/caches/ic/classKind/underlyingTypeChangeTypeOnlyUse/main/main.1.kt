import kotlin.test.*
import lib.*

@Test
fun runTest() {
    // 5000000000 does not fit an Int: the stale Int-ABI Holder truncates it.
    assertEquals(5000000000L, Holder(V(5000000000L)).v.x)
}
