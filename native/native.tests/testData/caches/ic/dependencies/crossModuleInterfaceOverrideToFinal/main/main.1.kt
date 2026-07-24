import kotlin.test.*
import test.*

@Test
fun runTest() {
    val child = Child()
    assertEquals("child-final", viaInterface(child))
    assertEquals("child-final", viaParent(child))
    assertEquals("child-final", viaChild(child))
}
