package org.jetbrains

import kotlin.test.Test
import kotlin.test.assertEquals

class HelloWorldKotlinTest {

    @Test
    fun greetingFromKotlin() {
        assertEquals("Hello, World!", getGreeting())
    }

    @Test
    fun greetingFromJava() {
        assertEquals("Hello from Java!", HelloWorldJava.getGreetingFromJava())
    }
}