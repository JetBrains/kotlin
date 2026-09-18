import kotlin.test.*
import test.*

@Test
fun runTest() {
    val child = Child()
    assertEquals(viaChild(child), "parent-open")
}
