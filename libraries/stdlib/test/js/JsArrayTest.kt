package testPackage

import org.junit.Test as test
import kotlin.test.*

class JsArrayTest {

    test fun arrays() {
        val a1 = array<String>()
        val a2 = array("foo")
        val a3 = array("foo", "bar")

        assertEquals(0, a1.size)
        assertEquals(1, a2.size)
        assertEquals(2, a3.size)

        assertEquals("[]", a1.toList().toString())
        assertEquals("[foo]", a2.toList().toString())
        assertEquals("[foo, bar]", a3.toList().toString())

    }
}
