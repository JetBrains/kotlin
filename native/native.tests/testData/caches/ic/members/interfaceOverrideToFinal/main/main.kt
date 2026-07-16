import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(1, viaInterface(C()))
    assertEquals(1, viaClass(C()))
}
