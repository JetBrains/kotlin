import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(1, f(A()))
    assertEquals(2, f(B()))
    assertEquals(0, f(C()))
}
