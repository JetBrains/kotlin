import kotlin.test.Test

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class PlatformIntTextExtensionsTest {
    @Test
    fun testToStringWithRadix() {
        assertPrints(pli(15).toString(2), "1111")
        assertPrints(pli(15).toString(8), "17")
        assertPrints(pli(15).toString(16), "f")
        assertPrints(plui(15u).toString(2), "1111")
        assertPrints(plui(15u).toString(8), "17")
        assertPrints(plui(15u).toString(16), "f")
    }
}