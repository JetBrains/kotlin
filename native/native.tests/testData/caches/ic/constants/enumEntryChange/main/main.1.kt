import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals("red_foo", foo(Color.RED))
    assertEquals("blue_foo", foo(Color.BLUE))
    assertEquals(0, yellowOrdinal())
    assertEquals("blue", blueValue())
}
