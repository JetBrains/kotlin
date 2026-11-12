/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.test.klib.runSanityTest
import org.jetbrains.kotlin.test.klib.runSanityTestNotMuted
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContains

class JsCodegenBoxSanity : AbstractJsCodegenBoxTest(
    testGroupOutputDirPrefix = "codegen/boxSanity/",
    pathToTestDir = TEST_DATA_ROOT,
) {
    @Test
    fun checkPassed() {
        runSanityTestNotMuted(TEST_DATA_ROOT + "green.kt")
    }

    @Test
    fun checkIncorrectBoxResult() {
        val exception = assertThrows<AssertionError> {
            runSanityTest(TEST_DATA_ROOT + "incorrectBoxResult.kt")
        }
        assertContains(exception.message!!, "Test failed with: FAIL. Expected <OK>, actual <FAIL>.")
    }

    @Test
    fun checkPassedIsolated() {
        runSanityTestNotMuted(TEST_DATA_ROOT + "greenIsolated.kt")
    }

    @Test
    fun checkIncorrectBoxResultIsolated() {
        val exception = assertThrows<AssertionError> {
            runSanityTest(TEST_DATA_ROOT + "incorrectBoxResultIsolated.kt")
        }
        assertContains(exception.message!!, "expected: <OK> but was: <FAIL>")
    }

    @Test
    fun checkGreenNeedsUnmuting() {
        val exception = assertThrows<AssertionError> {
            runSanityTest(TEST_DATA_ROOT + "greenNeedsUnmuting.kt")
        }
        assertContains(exception.message!!, "Looks like this test can be unmuted. Remove JS_IR from the IGNORE_BACKEND directive")
    }

    @Test
    fun checkMuted() {
        // `BlackBoxCodegenSuppressor` drops the failure of an ignored test, so the test passes rather than gets aborted.
        runSanityTest(TEST_DATA_ROOT + "muted.kt")
        val droppedFailure = nonGroupingRunner.failuresInterceptor.allFailedExceptions.single()
        assertContains(droppedFailure.cause.message!!, "expected: <OK> but was: <FAIL>")
    }

    private companion object {
        const val TEST_DATA_ROOT = "js/js.translator/testData/twoStageCodegenBoxSanity/"
    }
}
