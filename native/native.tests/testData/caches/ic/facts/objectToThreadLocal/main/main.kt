import kotlin.test.*
import test.*

@Test
fun runTest() {
    val first = bump()
    val second = bump()
    assertEquals(first + 1, second)
    assertEquals(second, Counter.n)
}
