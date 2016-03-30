package demo 

import org.testng.Assert.assertEquals
import org.testng.annotations.Test as test

class TestKotlinGreetingJoiner() {
    @test
    fun test() {
        val example : KotlinGreetingJoiner = KotlinGreetingJoiner(Greeter("Hi"))
        example.addName("Harry")
        example.addName("Ron")
        example.addName("Hermione")

        assertEquals(example.getJoinedGreeting(), "Hi Harry and Ron and Hermione")
    }
}

