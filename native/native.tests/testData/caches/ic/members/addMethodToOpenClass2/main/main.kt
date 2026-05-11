import kotlin.test.*
import test1.*
import test2.*

class C : B() {
    override fun bar() = "qzz"
}

@Test
fun runTest() {
    assertEquals("qzz", bar(C()))
}