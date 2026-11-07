import kotlin.test.*
import lib.*

@Test
fun runTest() {
    assertEquals(5, Holder(V(5)).v.x)
}
