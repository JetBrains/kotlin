/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteFinished
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.junit.Test

class TestLineEndings : TCServiceMessagesClientTest() {
    @Test
    fun test() {
        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
      StdOut[
text1

] // root//Test
      StdOut[

text2

] // root//Test
      FAILURE Expected <7>, actual <42>
 // root//Test
      StdOut[
text3

] // root//Test
      StdOut[

text4

] // root//Test
    COMPLETED FAILURE // root//Test
  COMPLETED FAILURE // root/
COMPLETED FAILURE // root
        """
        ) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("Test", false, null))
            regularText("\n\ntext1\n\n")
            regularText("\n\ntext2\n\n")
            serviceMessage(
                "testFailed",
                mapOf(
                    "name" to "Test",
                    "message" to "Expected <7>, actual <42>"
                )
            )
            regularText("\n\ntext3\n\n")
            regularText("\n\ntext4\n\n")
            serviceMessage(TestFinished("Test", 0))
            serviceMessage(TestSuiteFinished(""))
        }
    }
}