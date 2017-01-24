package org.jetbrains

import org.junit.*
import kotlin.test.*

open class SharedTest {
    @Test
    fun test0() {
        assertEquals("Hello, World!", getGreeting())
    }
}
