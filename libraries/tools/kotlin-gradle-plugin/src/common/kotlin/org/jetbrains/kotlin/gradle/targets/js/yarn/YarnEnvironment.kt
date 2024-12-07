/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment

data class YarnEnvironment(
    val executable: String,
    val standalone: Boolean,
    val ignoreScripts: Boolean,
    val yarnResolutions: List<YarnResolution>
) : PackageManagerEnvironment

internal val YarnEnv.asYarnEnvironment
    get() = YarnEnvironment(
        executable,
        standalone,
        ignoreScripts,
        yarnResolutions
    )