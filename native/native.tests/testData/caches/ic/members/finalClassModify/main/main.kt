import kotlin.test.*
import test.*

@Test
fun runTest() {
    val demo = Demo("a", "fixed")
    assertEquals("foo a", useFoo(demo))
}
