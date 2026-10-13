import kotlin.test.*
import lib.*

@Test
fun runTest() {
    Child.childComp
    assertEquals(expectedInitLog(), initLog)
}
