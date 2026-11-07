import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(7, pass(Wrapped(7)).n)
}
