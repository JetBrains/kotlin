import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals("1 foo", foo(Red()))
    assertEquals("5 foo", foo(Blue()))
}
