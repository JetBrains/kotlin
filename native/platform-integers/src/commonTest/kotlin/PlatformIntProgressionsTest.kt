import kotlin.test.Test

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class PlatformIntProgressionsTest {

    @Test
    fun testFromClosedRange() {
        assertPrints(PlatformIntProgression.fromClosedRange(pli(0), pli(4), pli(2)), "0..4 step 2")
        assertPrints(PlatformUIntProgression.fromClosedRange(plui(0u), plui(4u), pli(2)), "0..4 step 2")
    }

    @Test
    fun testFirst() {
        assertPrints(PlatformIntProgression.fromClosedRange(pli(0), pli(4), pli(2)).first, "0")
        assertPrints(PlatformUIntProgression.fromClosedRange(plui(0u), plui(4u), pli(2)).first, "0")
    }

    @Test
    fun testLast() {
        assertPrints(PlatformIntProgression.fromClosedRange(pli(0), pli(4), pli(2)).last, "4")
        assertPrints(PlatformUIntProgression.fromClosedRange(plui(0u), plui(4u), pli(2)).last, "4")
    }

    @Test
    fun testStep() {
        assertPrints(PlatformIntProgression.fromClosedRange(pli(0), pli(4), pli(2)).step, "2")
        assertPrints(PlatformUIntProgression.fromClosedRange(plui(0u), plui(4u), pli(2)).step, "2")
    }

    @Test
    fun testIsEmpty() {
        assertPrints(PlatformIntProgression.fromClosedRange(pli(0), pli(4), pli(2)).isEmpty(), "false")
        assertPrints(PlatformUIntProgression.fromClosedRange(plui(0u), plui(4u), pli(2)).isEmpty(), "false")
        assertPrints(PlatformIntProgression.fromClosedRange(pli(1), pli(0), pli(1)).isEmpty(), "true")
        assertPrints(PlatformUIntProgression.fromClosedRange(plui(1u), plui(0u), pli(1)).isEmpty(), "true")
    }

    @Test
    fun testDownTo() {
        assertPrints(pli(3) downTo pli(0), "3 downTo 0 step 1")
        assertPrints(plui(3u) downTo plui(0u), "3 downTo 0 step 1")
    }

    @Test
    fun testUntil() {
        assertPrints(pli(0) until pli(4), "0..3")
        assertPrints(plui(0u) until plui(4u), "0..3")
    }
}
