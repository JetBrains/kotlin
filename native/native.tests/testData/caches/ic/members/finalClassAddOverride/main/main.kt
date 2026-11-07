import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals("Base.foo", callFoo(Sub()))
}
