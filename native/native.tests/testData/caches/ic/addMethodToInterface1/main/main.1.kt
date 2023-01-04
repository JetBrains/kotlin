import kotlin.test.*
import test1.*
import test2.*

class IImpl : I {
    override fun baz() = "zzz"
    override fun foo() = 42
}

@Test
fun runTest() {
    assertEquals(42, bar(IImpl()))
}