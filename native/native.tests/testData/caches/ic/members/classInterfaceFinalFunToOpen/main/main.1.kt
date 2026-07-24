import kotlin.test.*
import test.*

@Test
fun runTest() {
    val child = Child()
    assertEquals("parent-open", viaInterface(child))
    assertEquals("parent-open", viaParent(child))
    assertEquals("parent-open", viaChild(child))
}
