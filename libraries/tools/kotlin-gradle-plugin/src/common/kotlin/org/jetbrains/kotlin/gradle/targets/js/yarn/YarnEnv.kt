/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.jetbrains.kotlin.gradle.targets.js.AbstractEnv
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

data class YarnEnv(
    override val download: Boolean,
    override val downloadBaseUrl: String?,
    override val cleanableStore: CleanableStore,
    override val dir: File,
    override val executable: String,
    override val ivyDependency: String,
    val ignoreScripts: Boolean,
    val yarnLockMismatchReport: YarnLockMismatchReport,
    val reportNewYarnLock: Boolean,
    val yarnLockAutoReplace: Boolean,
    val yarnResolutions: List<YarnResolution>
) : AbstractEnv {
    val standalone: Boolean
        get() = !download
}
