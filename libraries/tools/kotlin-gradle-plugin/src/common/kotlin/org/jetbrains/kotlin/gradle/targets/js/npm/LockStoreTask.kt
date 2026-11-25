/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.contentEqualsIgnoringLineEndings
import org.jetbrains.kotlin.gradle.utils.unifiedDiff

@DisableCachingByDefault
abstract class LockStoreTask : LockCopyTask() {
    @get:Input
    abstract val lockFileMismatchReport: Property<LockFileMismatchReport>

    @get:Input
    abstract val reportNewLockFile: Property<Boolean>

    @get:Input
    abstract val lockFileAutoReplace: Property<Boolean>

    @get:Internal
    internal abstract val mismatchMessage: Property<String>

    override fun copy() {
        val outputFile = outputDirectory.get().asFile.resolve(fileName.get())

        val value = inputFile.orNull
        requireNotNull(value) {
            "Input file $fileName should exist"
        }

        val shouldReportMismatch = if (!outputFile.exists()) {
            reportNewLockFile.get()
        } else {
            lockFileMismatchReport.get() != LockFileMismatchReport.NONE && !contentEqualsIgnoringLineEndings(value.asFile, outputFile)
        }

        // must be computed before the copy, because the stored file can be replaced
        val mismatchDiff = if (shouldReportMismatch && outputFile.exists()) {
            unifiedDiff(expected = outputFile, actual = value.asFile)
        } else {
            null
        }

        // outputFile is updated only with auto replace or not existed, but we need to delete all other files initially
        fs.delete {
            it.delete(additionalInputFiles)
        }

        if (!outputFile.exists() || lockFileAutoReplace.get()) {
            super.copy()
        }

        if (shouldReportMismatch) {
            logger.info("[$path] Detected mismatch for inputFile:${inputFile.get().asFile}, outputFile:${outputFile}, lockFileMismatchReport:${lockFileMismatchReport.get()}")
            val message = if (mismatchDiff.isNullOrEmpty()) {
                mismatchMessage.get()
            } else {
                mismatchMessage.get() + "\n\n" + LOCK_FILE_DIFF_HEADER + "\n" + mismatchDiff
            }
            when (lockFileMismatchReport.get()) {
                LockFileMismatchReport.NONE -> {}
                LockFileMismatchReport.WARNING -> {
                    logger.warn(message)
                }
                LockFileMismatchReport.FAIL -> throw GradleException(message)
            }
        }
    }
}

private const val LOCK_FILE_DIFF_HEADER = "Difference between stored and actual lock files:"
