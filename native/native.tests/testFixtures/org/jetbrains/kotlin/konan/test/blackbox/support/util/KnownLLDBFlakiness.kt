/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assumeFalse

/**
 * Skips the test if the LLDB output contains a marker of a known failure unrelated to the code under test.
 *
 * Driving a real lldb process makes a test fail sporadically, no matter how its output is verified afterwards.
 * So every LLDB-driven test case applies this check, which runs before any check that could report the very
 * same output as a failure.
 *
 * The check is immutable, so the single instance is shared by all the test cases.
 */
val knownLldbFlakinessMute: TestRunCheck.MuteKnownFlakiness = TestRunCheck.MuteKnownFlakiness { runResult ->
    val lldbOutput = runResult.processOutputAsString(TestRunCheck.Output.ALL)

    // Workaround for KT-84923. Mute the test if the problem occurs:
    val kt84923Message = "attached to process, but could not pause execution; attach failed"
    assumeFalse(lldbOutput.contains(kt84923Message)) { "Test skipped because of KT-84923" }

    // Workaround for KT-87414: an lldb stepping race can leave a step-into command stuck at an already deleted internal breakpoint;
    // the watchdog in konan_lldb_test_helper.py converts the resulting hang into this failure. Both markers are required:
    // the watchdog/ message alone may also indicate a genuine deadlock in the tested program, which must still fail the test.
    val stuckStepMessage = "A single step-into command didn't complete"
    val deletedBreakpointMessage = "which has been deleted"
    assumeFalse(lldbOutput.contains(stuckStepMessage) && lldbOutput.contains(deletedBreakpointMessage)) {
        "Test skipped because of KT-87414"
    }
}
