import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals("class-open", callFoo(Child()))
}
