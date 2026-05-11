import kotlin.test.*
import test.*

@Test
fun runTest() {
    val f = FooImpl()
    assertEquals(3, bar(f))
}
