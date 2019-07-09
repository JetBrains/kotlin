package com.example.foo

import com.example.thirdparty.thirdPartyFun
import kotlin.test.Test
import kotlin.test.assertEquals

class FooJvmAndJsTest {
    @Test
    fun testFooJvmAndJs() {
        assertEquals(fooJvmAndJs(), foo())
    }

    @Test
    fun testThirdParty() {
        thirdPartyFun()
    }
}