/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.slf4j.Logger

internal class JSServiceMessagesClient(
    results: TestResultProcessor,
    settings: TCServiceMessagesClientSettings,
    log: Logger
) : TCServiceMessagesClient(results, settings, log) {
    override fun printNonTestOutput(text: String) {
        // do nothing
    }
}