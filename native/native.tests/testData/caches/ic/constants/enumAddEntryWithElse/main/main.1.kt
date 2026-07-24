import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals("a", name(E.A))
    assertEquals("b", name(E.B))
    assertEquals("other", name(E.C))
}
