import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(1, foo(B()))
    assertEquals(2, foo(C()))
}
