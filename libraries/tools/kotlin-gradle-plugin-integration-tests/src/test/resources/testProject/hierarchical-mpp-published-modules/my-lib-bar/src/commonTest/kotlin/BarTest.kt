package com.example.bar

import com.example.foo.foo
import com.example.foo.fooCommon
import kotlin.test.Test
import kotlin.test.assertEquals

class BarTest {
    @Test
    fun testBar() {
        // thirdPartyFun() // unresolved
        // fooJvmAndJs() // unresolved
        // fooLinuxAndJs() // unresolved
        assertEquals(foo(), fooCommon())
        assertEquals(bar(), barCommon())
    }
}