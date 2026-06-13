import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals("1 foo", foo(Red()))
    assertEquals("2 foo", foo(Blue()))
}
