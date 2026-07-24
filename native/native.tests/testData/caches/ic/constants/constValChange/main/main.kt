import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(2, foo(1))
    assertEquals(3, inlineUse())
    assertEquals("value", constInWhen(1))
}
