/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.contentEquals

@DisableCachingByDefault
abstract class LockStoreTask : LockCopyTask() {
    @get:Input
    abstract val lockFileMismatchReport: Property<LockFileMismatchReport>

    @get:Input
    abstract val reportNewLockFile: Property<Boolean>

    @get:Input
    abstract val lockFileAutoReplace: Property<Boolean>

    override fun copy() {
        val outputFile = outputDirectory.get().asFile.resolve(fileName.get())

        val value = inputFile.orNull
        requireNotNull(value) {
            "Input file $fileName should exist"
        }

        val shouldReportMismatch = if (!outputFile.exists()) {
            reportNewLockFile.get()
        } else {
            lockFileMismatchReport.get() != LockFileMismatchReport.NONE && !contentEquals(value.asFile, outputFile)
        }

        // outputFile is updated only with auto replace or not existed, but we need delete all other files initially
        fs.delete {
            it.delete(additionalInputFiles)
        }

        if (!outputFile.exists() || lockFileAutoReplace.get()) {
            super.copy()
        }

        if (shouldReportMismatch) {
            when (lockFileMismatchReport.get()) {
                LockFileMismatchReport.NONE -> {}
                LockFileMismatchReport.WARNING -> {
                    logger.warn(PACKAGE_LOCK_MISMATCH_MESSAGE)
                }
                LockFileMismatchReport.FAIL -> throw GradleException(PACKAGE_LOCK_MISMATCH_MESSAGE)
                else -> error("Unknown mismatch report kind")
            }
        }
    }
}