import kotlin.test.*
import app.*

@Test
fun runTest() {
    val d = Data(1)
    assertEquals(3, aValue(d))
    assertEquals(2, bValue(d))
}
