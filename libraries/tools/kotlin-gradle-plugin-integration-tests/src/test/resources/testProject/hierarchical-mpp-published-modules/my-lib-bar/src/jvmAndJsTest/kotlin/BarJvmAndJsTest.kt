package com.example.bar

import com.example.foo.foo
import com.example.thirdparty.thirdPartyFun
import kotlin.test.Test
import kotlin.test.assertEquals

class BarJvmAndJsTest {
    @Test
    fun testBarJvmAndJs() {
        // barLinuxAndJs() // unresolved
        assertEquals(foo(), barJvmAndJs())
    }

    @Test
    fun testThirdParty() {
        thirdPartyFun()
    }
}