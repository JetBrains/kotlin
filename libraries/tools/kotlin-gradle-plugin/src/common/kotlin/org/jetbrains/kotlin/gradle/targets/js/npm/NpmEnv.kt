/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

data class NpmEnv(
    val executable: String,
    val ignoreScripts: Boolean,
    val standalone: Boolean,
    val packageLockMismatchReport: LockFileMismatchReport,
    val reportNewPackageLock: Boolean,
    val packageLockAutoReplace: Boolean,
    val overrides: List<NpmOverride>
)