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

@DisableCachingByDefault
abstract class YarnLockCopyTask : LockCopyTask()

@DisableCachingByDefault
abstract class YarnLockStoreTask : LockStoreTask() {
    @Internal
    val yarnLockMismatchReport: Provider<YarnLockMismatchReport> = lockFileMismatchReport.map { it.fromLockFileMismatchReport() }

    @Internal
    val reportNewYarnLock: Provider<Boolean> = reportNewLockFile

    @Internal
    val yarnLockAutoReplace: Provider<Boolean> = lockFileAutoReplace
}

enum class YarnLockMismatchReport {
    NONE,
    WARNING,
    FAIL;

    fun toLockFileMismatchReport(): LockFileMismatchReport =
        when(this) {
            NONE -> LockFileMismatchReport.NONE
            WARNING -> LockFileMismatchReport.WARNING
            FAIL -> LockFileMismatchReport.FAIL
        }
}

fun LockFileMismatchReport.fromLockFileMismatchReport(): YarnLockMismatchReport =
    when(this) {
        LockFileMismatchReport.NONE -> YarnLockMismatchReport.NONE
        LockFileMismatchReport.WARNING -> YarnLockMismatchReport.WARNING
        LockFileMismatchReport.FAIL -> YarnLockMismatchReport.FAIL
    }