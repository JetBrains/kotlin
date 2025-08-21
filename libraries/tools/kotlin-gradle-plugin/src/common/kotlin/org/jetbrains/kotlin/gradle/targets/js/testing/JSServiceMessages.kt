/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.jetbrains.kotlin.gradle.internal.LogType
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.slf4j.Logger

internal open class JSServiceMessagesTestExecutionSpec(
    processLaunchOpts: ProcessLaunchOptions,
    processArgs: List<String>,
    checkExitCode: Boolean,
    clientSettings: TCServiceMessagesClientSettings,
) : TCServiceMessagesTestExecutionSpec(
    processLaunchOpts,
    processArgs,
    checkExitCode,
    clientSettings,
) {

    override fun createClient(testResultProcessor: TestResultProcessor, log: Logger): TCServiceMessagesClient {
        return JSServiceMessagesClient(
            results = testResultProcessor,
            settings = clientSettings,
            log = log,
        )
    }
}

internal open class JSServiceMessagesClient(
    results: TestResultProcessor,
    settings: TCServiceMessagesClientSettings,
    log: Logger,
) : TCServiceMessagesClient(results, settings, log) {
    override fun printNonTestOutput(text: String, type: LogType?) {
        if (log.isDebugEnabled) {
            log.debug(text)
        }
    }
}
