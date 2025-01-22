/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandle
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.slf4j.Logger

internal class NativeAppleSimulatorTCServiceMessagesTestExecutionSpec(
    processLaunchOpts: ProcessLaunchOptions,
    processArgs: List<String>,
    checkExitCode: Boolean,
    clientSettings: TCServiceMessagesClientSettings,
    dryRunArgs: List<String>?,
    private val standaloneMode: Provider<Boolean>,
) : TCServiceMessagesTestExecutionSpec(
    processLaunchOpts,
    processArgs,
    checkExitCode,
    clientSettings,
    dryRunArgs,
) {
    override fun createClient(
        testResultProcessor: TestResultProcessor,
        log: Logger,
    ): TCServiceMessagesClient {
        return NativeAppleSimulatorTCServiceMessagesClient(testResultProcessor, clientSettings, log, standaloneMode)
    }
}

internal class NativeAppleSimulatorTCServiceMessagesClient(
    results: TestResultProcessor,
    settings: TCServiceMessagesClientSettings,
    log: Logger,
    private val standaloneMode: Provider<Boolean>,
) : TCServiceMessagesClient(results, settings, log) {
    override fun testFailedMessage(execHandle: ExecHandle, exitValue: Int) = when {
        !standaloneMode.get() && exitValue == 149 -> """
                You have standalone simulator tests run mode disabled and tests have failed to run.
                The problem can be that you have not booted the required device or have configured the task to a different simulator. Please check the task output and its device configuration.
                If you are sure that your setup is correct, please file an issue: https://kotl.in/issue
            """.trimIndent()
        else -> super.testFailedMessage(execHandle, exitValue)
    }
}
