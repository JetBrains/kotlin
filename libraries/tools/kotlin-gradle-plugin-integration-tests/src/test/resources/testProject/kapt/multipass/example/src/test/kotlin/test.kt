package example

import org.junit.Test
import org.junit.Assert.*
import generated.TestClass123

class AnnotationTest {
    @Test fun testSimple() {
        assertEquals("TestClass123", TestClass123::class.java.simpleName)
    }
}