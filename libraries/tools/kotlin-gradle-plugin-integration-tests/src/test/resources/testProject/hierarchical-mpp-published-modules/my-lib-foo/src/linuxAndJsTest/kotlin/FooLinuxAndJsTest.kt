package com.example.foo

class FooLinuxAndJsTest {
    @Test
    fun testFooJvmAndJs() {
        assertEquals(foo(), fooCommon())
        fooLinuxAndJs()
    }
}