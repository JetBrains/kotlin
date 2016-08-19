package foo

import org.junit.Test
import org.junit.Assert

class GreeterTest {
    @Test
    fun testHelloWorld() {
        Assert.assertEquals("Hello World!", Greeter("World").greeting)
    }
}