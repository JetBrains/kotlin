package example

import org.junit.Assert
import org.junit.Test

class KotlinTest {
    @Test
    fun test() {
        val testClass = TestClass()
        Assert.assertEquals("text", testClass.testVal)
    }
}