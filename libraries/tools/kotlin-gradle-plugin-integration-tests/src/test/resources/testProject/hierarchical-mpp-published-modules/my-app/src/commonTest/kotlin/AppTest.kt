package com.example.app

import com.example.bar.bar
import com.example.bar.barCommon
import com.example.foo.foo
import com.example.foo.fooCommon
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
    @Test
    fun testApp() {
        appCommon()
        assertEquals(foo(), fooCommon())
        assertEquals(bar(), barCommon())
        // fooJvmAndJs() // unresolved
        // fooLinuxAndJs() // unresolved
        // barJvmAndJs // unresolved
    }
}