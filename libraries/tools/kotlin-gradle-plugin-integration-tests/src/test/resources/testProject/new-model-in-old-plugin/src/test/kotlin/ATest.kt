package com.example

import kotlin.test.*

class ATest {
    @Test
    fun testF() {
        val f = A().f()
        assertEquals("hello", f)
    }
}
