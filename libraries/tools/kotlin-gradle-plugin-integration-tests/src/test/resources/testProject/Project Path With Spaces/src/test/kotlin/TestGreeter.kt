package demo

import org.testng.Assert.assertEquals

class TestGreeter {
    fun test() {
       val greeter = Greeter("Hi!")
        assertEquals("Hi!", greeter.greeting)
    }
}