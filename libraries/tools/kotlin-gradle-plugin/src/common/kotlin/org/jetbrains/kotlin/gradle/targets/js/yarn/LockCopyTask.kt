/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.npm.LockStoreTask
import java.io.File

@DisableCachingByDefault
abstract class YarnLockCopyTask : LockCopyTask()

/**
 * Updates the project's stored `yarn.lock` file.
 * When this task runs the existing `yarn.lock` file will be overwritten.
 *
 * If no NPM dependencies are present, this task will delete the existing lock file.
 *
 * See https://kotl.in/js-project-setup/version-locking
 */
@DisableCachingByDefault
abstract class YarnLockUpgradeTask internal constructor() : YarnLockCopyTask() {

    override fun copy() {
        val inputFile = inputFile.getOrNull()?.asFile

        val isInputLockFileEmpty = isEmptyYarnLock(inputFile)

        if (!isInputLockFileEmpty) {
            super.copy()
        } else {
            val outputDirectory = outputDirectory.get().asFile
            val outputFile = outputDirectory.resolve(fileName.get())
            if (outputFile.exists()) {
                logger.lifecycle("[$path] No NPM dependencies detected. Deleting empty yarn.lock file $outputFile")
                fs.delete {
                    it.delete(outputFile)
                }
            }
        }
    }
}

/**
 * Validates and the project's stored `yarn.lock` file against the actual current state of NPM dependencies.
 * If the stored lock file is valid, the task will update it.
 *
 * To overwrite the stored lockfile without validation use [YarnLockUpgradeTask].
 *
 * See https://kotl.in/js-project-setup/version-locking
 */
@DisableCachingByDefault
abstract class YarnLockStoreTask : LockStoreTask() {
    @get:Internal
    val yarnLockMismatchReport: Provider<YarnLockMismatchReport>
        get() = lockFileMismatchReport.map { it.fromLockFileMismatchReport() }

    @get:Internal
    val reportNewYarnLock: Provider<Boolean>
        get() = reportNewLockFile

    @get:Internal
    val yarnLockAutoReplace: Provider<Boolean>
        get() = lockFileAutoReplace

    override fun copy() {
        val inputFile = inputFile.getOrNull()?.asFile
        val outputFile = outputDirectory.get().asFile.resolve(fileName.get())

        if (isEmptyYarnLock(inputFile) && !outputFile.exists()) {
            logger.info("[$path] No NPM dependencies detected, and stored lockfile does not exist - skipping copy $inputFile")
            return
        } else {
            super.copy()
        }
    }
}

private fun isEmptyYarnLock(file: File?): Boolean =
    file == null ||
            !file.exists() ||
            file.useLines { lines ->
                lines.all { it.startsWith("#") || it.isBlank() }
            }

enum class YarnLockMismatchReport {
    NONE,
    WARNING,
    FAIL;

    fun toLockFileMismatchReport(): LockFileMismatchReport =
        when (this) {
            NONE -> LockFileMismatchReport.NONE
            WARNING -> LockFileMismatchReport.WARNING
            FAIL -> LockFileMismatchReport.FAIL
        }
}

fun LockFileMismatchReport.fromLockFileMismatchReport(): YarnLockMismatchReport =
    when (this) {
        LockFileMismatchReport.NONE -> YarnLockMismatchReport.NONE
        LockFileMismatchReport.WARNING -> YarnLockMismatchReport.WARNING
        LockFileMismatchReport.FAIL -> YarnLockMismatchReport.FAIL
    }
