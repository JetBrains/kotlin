/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.slf4j.Logger

/**
 * Lightweight implementation of [TCServiceMessagesClient] that allows appending [currentRunnerName] to [testNameSuffix].
 */
internal class PlaywrightTCServiceMessagesClient(
    results: TestResultProcessor,
    settings: TCServiceMessagesClientSettings,
    log: Logger,
) : TCServiceMessagesClient(results, settings, log) {
    var currentRunnerName: String? = null

    override val testNameSuffix: String?
        get() = super.testNameSuffix.let { superTestNameSuffix ->
            when {
                currentRunnerName == null -> superTestNameSuffix
                superTestNameSuffix == null -> currentRunnerName
                else -> "$superTestNameSuffix, $currentRunnerName"
            }
        }
}
