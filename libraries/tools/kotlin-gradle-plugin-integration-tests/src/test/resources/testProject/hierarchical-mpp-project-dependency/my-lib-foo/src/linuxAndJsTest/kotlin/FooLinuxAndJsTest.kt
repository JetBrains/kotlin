package com.example.foo

import kotlin.test.*

class FooLinuxAndJsTest {
    @Test
    fun testFooJvmAndJs() {
        assertEquals(foo(), fooCommon())
        fooLinuxAndJs()
    }
}