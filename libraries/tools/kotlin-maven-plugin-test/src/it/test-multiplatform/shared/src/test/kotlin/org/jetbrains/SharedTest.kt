package org.jetbrains

import kotlin.test.*

open class SharedTest {
    @Test
    fun test0() {
        assertEquals("Hello, World!", getGreeting())
    }
}
