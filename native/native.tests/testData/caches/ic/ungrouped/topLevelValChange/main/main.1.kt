import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(3, bar())
    assertEquals(4, inlineUse())
}
