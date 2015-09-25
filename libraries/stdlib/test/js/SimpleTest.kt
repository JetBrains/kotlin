package testPackage

import org.junit.Test as test
import kotlin.test.*

class SimpleTest {

    public fun testFoo() {
        val name = "world"
        val message = "hello $name!"
        assertEquals("hello world!", message)
    }

    @test fun cheese() {
        val name = "world"
        val message = "bye $name!"
        assertEquals("bye world!", message)
    }
}
