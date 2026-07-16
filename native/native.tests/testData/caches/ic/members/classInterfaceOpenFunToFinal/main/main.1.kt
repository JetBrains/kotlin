import kotlin.test.*
import test.*

@Test
fun runTest() {
    val child = Child()
    assertEquals("parent-final", viaInterface(child))
    assertEquals("parent-final", viaParent(child))
    assertEquals("parent-final", viaChild(child))
}
