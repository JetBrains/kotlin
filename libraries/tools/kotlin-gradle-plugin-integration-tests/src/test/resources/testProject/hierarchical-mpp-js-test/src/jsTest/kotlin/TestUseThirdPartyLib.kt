package com.example

import com.example.thirdparty.thirdPartyFun
import kotlin.test.*

class TestUseCoroutines {
    @Test
    fun testThirdPartyLibAvailable() {
        assertEquals("thirdPartyFun @ js", thirdPartyFun())
    }
}