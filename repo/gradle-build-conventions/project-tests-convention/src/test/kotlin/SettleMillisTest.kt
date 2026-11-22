/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The shape of the settle: proportional to what a replay wrote, capped at what the buffers can
 * hold, and bounded per build so the event thread still drains in the time the build gives it.
 */
class SettleMillisTest {

    @Test
    fun `the settle is the drain time of what was written`() {
        // 391 chars per millisecond is the rate the agent ingests a step's output at.
        assertEquals(1_000, settleMillisFor(emittedChars = 391_000, alreadySpentMillis = 0))
        assertEquals(7, settleMillisFor(emittedChars = 2_900, alreadySpentMillis = 0))
    }

    @Test
    fun `a replay that wrote nothing sleeps nothing`() {
        assertEquals(0, settleMillisFor(emittedChars = 0, alreadySpentMillis = 0))
        // Less than the agent reads in a millisecond is not worth a sleep either.
        assertEquals(0, settleMillisFor(emittedChars = 300, alreadySpentMillis = 0))
    }

    @Test
    fun `no settle outlasts the buffers between the processes`() {
        // About a megabyte is all they hold; more sleep cannot deliver more.
        assertEquals(3_000, settleMillisFor(emittedChars = 100_000_000, alreadySpentMillis = 0))
    }

    @Test
    fun `a build settles for no more than its budget, shared over the tasks that need it`() {
        // The 60 s the build gives its event thread: half for the settles, half for the printing.
        assertEquals(2_000, settleMillisFor(emittedChars = 1_000_000, alreadySpentMillis = 28_000))
        assertEquals(0, settleMillisFor(emittedChars = 1_000_000, alreadySpentMillis = 30_000))
        assertEquals(0, settleMillisFor(emittedChars = 1_000_000, alreadySpentMillis = 31_000))
    }
}
