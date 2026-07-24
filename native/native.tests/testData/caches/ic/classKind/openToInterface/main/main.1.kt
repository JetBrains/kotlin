import kotlin.test.*
import test.*

@Test
fun runTest() {
    val alpha = AlphaImpl()
    assertEquals(40, qux(alpha))
}
