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
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.YARN_LOCK_MISMATCH_MESSAGE

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

    override val mismatchMessage: String
        get() = YARN_LOCK_MISMATCH_MESSAGE
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