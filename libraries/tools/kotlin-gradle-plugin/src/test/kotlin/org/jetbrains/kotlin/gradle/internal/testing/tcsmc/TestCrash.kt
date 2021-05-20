/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.junit.Assert.assertEquals
import org.junit.Test

class TestCrash : TCServiceMessagesClientTest() {
    @Test
    fun testNativeCrash() {
        treatFailedTestOutputAsStacktrace = true

        var e: Throwable? = null
        try {
            assertEvents(
                """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
      StdOut[Crash info] // root//Test
      FAILURE  // root//Test
    COMPLETED FAILURE // root//Test
  COMPLETED FAILURE // root/
COMPLETED FAILURE // root                
            """.trimIndent()
            ) {
                serviceMessage(TestSuiteStarted(""))
                serviceMessage(TestStarted("Test", false, null))
                regularText(
                    """Crash info"""
                )
            }
        } catch (t: Throwable) {
            e = t
        }

        assertEquals(
            "Test running process exited unexpectedly.\n" +
                    "Current test: Test\n" +
                    "Process output:\n" +
                    " Crash info",
            e?.message
        )
    }
}