/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.jetbrains.kotlin.gradle.targets.js.AbstractEnv
import java.io.File

data class D8Env(
    override val download: Boolean,
    override val downloadBaseUrl: String?,
    override val allowInsecureProtocol: Boolean,
    override val ivyDependency: String,
    override val executable: String,
    override val dir: File,
    val isWindows: Boolean,
) : AbstractEnv {
    val executablePath: File
        get() = File(executable)
}