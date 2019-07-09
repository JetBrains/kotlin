package com.example.bar

import com.example.foo.fooCommon
import com.example.foo.fooLinuxAndJs
import kotlin.test.Test

class BarLinuxAndJsTest {
    @Test
    fun testBarLinuxAndJs() {
        fooCommon()
        fooLinuxAndJs()
        // fooJvmAndJs() // unresolved
        // thirdPartyFun() // unresolved
        barLinuxAndJs()
    }
}