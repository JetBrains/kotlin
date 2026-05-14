import kotlin.test.*
import test.*

@Test
fun runTest() {
    val c = C()
    bar(c)
    assertEquals(45, c.x)
}
