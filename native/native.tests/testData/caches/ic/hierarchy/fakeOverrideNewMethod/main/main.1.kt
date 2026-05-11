import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(1, callFoo(Derived()))
    assertEquals(2, Derived().bar())
}

