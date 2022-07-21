/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.jetbrains.kotlin.gradle.utils.contentEquals

abstract class YarnLockMismatchReportService : BuildService<YarnLockMismatchReportService.Params>, AutoCloseable,
    OperationCompletionListener {
    @Volatile
    private var shouldFailOnClose: Boolean = false

    // Some parameters for the web server
    internal interface Params : BuildServiceParameters {
        val inputFile: RegularFileProperty
        val outputFile: RegularFileProperty
        val shouldFailOnClose: Property<Boolean>
        val reportNewYarnLock: Property<Boolean>
    }

    fun failOnClose() {
        shouldFailOnClose = true
    }

    override fun onFinish(event: FinishEvent?) {
        // noop
    }

    override fun close() {
        if (!parameters.inputFile.get().asFile.exists()) {
            return
        }

        if (parameters.reportNewYarnLock.get() && !parameters.outputFile.get().asFile.exists()) {
            throw GradleException(YARN_LOCK_MISMATCH_MESSAGE)
        }

        if (shouldFailOnClose || parameters.shouldFailOnClose.get() && !contentEquals(parameters.inputFile.get().asFile, parameters.outputFile.get().asFile)) {
            throw GradleException(YARN_LOCK_MISMATCH_MESSAGE)
        }
    }
}

val YARN_LOCK_MISMATCH_MESSAGE = "yarn.lock was changed. Run `kotlinActualizeYarnLock` task to actualize yarn.lock file"