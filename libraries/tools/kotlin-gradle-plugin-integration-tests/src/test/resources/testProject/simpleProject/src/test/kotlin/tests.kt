package demo 

import org.testng.Assert.*
import org.testng.annotations.Test as test

class TestSource() {
    @test fun f() {
        val example : KotlinGreetingJoiner = KotlinGreetingJoiner(Greeter("Hi"))
        example.addName("Harry")
        example.addName("Ron")
        example.addName(null)
        example.addName("Hermione")

        assertEquals(example.getJoinedGreeting(), "Hi Harry and Ron and Hermione")
        assertEquals(example.getNames().size, 4)
    }
}

