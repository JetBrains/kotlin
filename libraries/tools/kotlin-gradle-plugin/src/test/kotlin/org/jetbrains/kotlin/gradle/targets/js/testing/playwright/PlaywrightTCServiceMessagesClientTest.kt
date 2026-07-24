/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteFinished
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.jetbrains.kotlin.gradle.internal.testing.RecordingTestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaywrightTCServiceMessagesClientTest {
    @Test
    fun `no test name suffix or current runner name provided`() {
        assertClientProducesOutput(
            expectedOutput = """
            STARTED SUITE root // root
              STARTED SUITE  // root/
                STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
                COMPLETED SUCCESS // root//Test
              COMPLETED SUCCESS // root/
            COMPLETED SUCCESS // root
            """
        )
    }

    @Test
    fun `no current runner name provided`() {
        assertClientProducesOutput(
            testNameSuffix = "defaultSuffix",
            expectedOutput = """
            STARTED SUITE root // root
              STARTED SUITE  // root/
                STARTED TEST displayName: Test[defaultSuffix], classDisplayName: , className: , name: Test // root//Test
                COMPLETED SUCCESS // root//Test
              COMPLETED SUCCESS // root/
            COMPLETED SUCCESS // root
            """
        )
    }

    @Test
    fun `no default test name suffix provided`() {
        assertClientProducesOutput(
            currentRunnerName = "browser",
            expectedOutput = """
            STARTED SUITE root // root
              STARTED SUITE  // root/
                STARTED TEST displayName: Test[browser], classDisplayName: , className: , name: Test // root//Test
                COMPLETED SUCCESS // root//Test
              COMPLETED SUCCESS // root/
            COMPLETED SUCCESS // root
            """
        )
    }

    @Test
    fun `default test name suffix and current runner name provided`() {
        assertClientProducesOutput(
            testNameSuffix = "defaultSuffix",
            currentRunnerName = "browser",
            expectedOutput = """
            STARTED SUITE root // root
              STARTED SUITE  // root/
                STARTED TEST displayName: Test[defaultSuffix, browser], classDisplayName: , className: , name: Test // root//Test
                COMPLETED SUCCESS // root//Test
              COMPLETED SUCCESS // root/
            COMPLETED SUCCESS // root
            """
        )
    }

    private fun assertClientProducesOutput(
        testNameSuffix: String? = null,
        currentRunnerName: String? = null,
        expectedOutput: String
    ) {
        val results = RecordingTestResultProcessor()
        val client = PlaywrightTCServiceMessagesClient(
            results = results,
            settings = TCServiceMessagesClientSettings(
                rootNodeName = "root",
                testNameSuffix = testNameSuffix,
            ),
            log = LoggerFactory.getLogger("test"),
        )

        client.root {
            client.currentRunnerName = currentRunnerName
            client.serviceMessage(TestSuiteStarted(""))
            client.serviceMessage(TestStarted("Test", false, null))
            client.serviceMessage(TestFinished("Test", 0))
            client.serviceMessage(TestSuiteFinished(""))
        }

        assertEquals(
            expectedOutput.trimIndent(),
            results.output.toString().trim()
        )
    }
}
