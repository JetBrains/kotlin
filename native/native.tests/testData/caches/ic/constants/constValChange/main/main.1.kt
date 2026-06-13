import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(4, foo(2))
    assertEquals(4, inlineUse())
    assertEquals("value", constInWhen(2))
}
