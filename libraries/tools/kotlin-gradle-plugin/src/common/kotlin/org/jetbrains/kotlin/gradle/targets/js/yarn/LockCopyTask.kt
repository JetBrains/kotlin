/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.npm.LockStoreTask
import java.io.File

@DisableCachingByDefault
abstract class YarnLockCopyTask : LockCopyTask()

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

    @get:Input
    abstract val storeEmptyLockFile: Property<Boolean>

    override fun copy() {
        if (storeEmptyLockFile.get()) {
            super.copy()
            return
        }

        val isEmptyYarnLock = inputFile.getOrNull()?.let { regularFile ->
            regularFile.asFile.useLines { lines ->
                lines.all { it.startsWith("#") || it.isBlank() }
            }
        } ?: true

        if (!isEmptyYarnLock) {
            super.copy()
        }
    }
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