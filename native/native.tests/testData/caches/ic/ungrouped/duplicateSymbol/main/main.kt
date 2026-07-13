import kotlin.test.*
import app.*

@Test
fun runTest() {
    val d = Data(1)
    assertEquals(2, aValue(d))
    assertEquals(2, bValue(d))
}
