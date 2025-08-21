/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.yarn

import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootEnvSpec

/**
 * Spec for Yarn - package manager to install NPM dependencies
 */
abstract class WasmYarnRootEnvSpec internal constructor() : BaseYarnRootEnvSpec() {
    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        val YARN: String
            get() = extensionName("yarnSpec")
    }

}