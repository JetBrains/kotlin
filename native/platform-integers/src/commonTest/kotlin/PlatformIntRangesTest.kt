import kotlin.test.Test

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class PlatformIntRangesTest {

    @Test
    fun testConstructor() {
        assertPrints(PlatformIntRange(pli(0), pli(3)), "0..3")
        assertPrints(PlatformUIntRange(plui(0u), plui(3u)), "0..3")
    }

    @Test
    fun testEmpty() {
        assertPrints(PlatformIntRange.EMPTY.isEmpty(), "true")
        assertPrints(PlatformUIntRange.EMPTY.isEmpty(), "true")
    }
}