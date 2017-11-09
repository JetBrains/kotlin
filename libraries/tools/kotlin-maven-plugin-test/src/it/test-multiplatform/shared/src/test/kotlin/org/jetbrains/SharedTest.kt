package org.jetbrains

import kotlin.test.*
import org.junit.Test

open class SharedTest {
    @Test
    fun test0() {
        assertEquals("Hello, World!", getGreeting())
    }
}
