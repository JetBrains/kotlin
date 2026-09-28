import kotlin.test.*
import user.*

@Test
fun doTest() {
    assertTrue(independent() > 0)
    assertTrue(callsLib() > 0)
}
