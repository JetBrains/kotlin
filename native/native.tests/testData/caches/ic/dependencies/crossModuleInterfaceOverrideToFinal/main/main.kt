import kotlin.test.*
import test.*

@Test
fun runTest() {
    val child = Child()
    assertEquals("child-open", viaInterface(child))
    assertEquals("child-open", viaParent(child))
    assertEquals("child-open", viaChild(child))
}
