package com.example.foo

import kotlin.test.Test
import kotlin.test.assertEquals

class FooTest {
    @Test
    fun testFoo() {
        assertEquals(foo(), fooCommon())
    }
}