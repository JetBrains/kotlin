package org.jetbrains

import org.junit.*

open class SharedTest {
    @Test
    fun test0() {
        assertEquals("Hello, World!", getGreeting())
    }
}