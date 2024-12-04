/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment

data class NpmEnvironment(
    val executable: String,
    val ignoreScripts: Boolean,
    val standalone: Boolean,
    val overrides: List<NpmOverride>
) : PackageManagerEnvironment

internal val NpmEnv.asNpmEnvironment
    get() = NpmEnvironment(
        executable,
        ignoreScripts,
        standalone,
        overrides
    )