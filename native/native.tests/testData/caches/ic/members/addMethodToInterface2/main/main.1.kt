import kotlin.test.*
import test1.*
import test2.*

class JImpl : J {
    override fun baz() = "qxx"
    override fun foo() = 42
    override fun bar() = "zzz"
}

@Test
fun runTest() {
    assertEquals("zzz", bar(JImpl()))
}