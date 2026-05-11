import kotlin.test.*
import test1.*
import test2.*

class B : A() {
    override fun foo() = 42
}

@Test
fun runTest() {
    assertEquals(42, bar(B()))
}