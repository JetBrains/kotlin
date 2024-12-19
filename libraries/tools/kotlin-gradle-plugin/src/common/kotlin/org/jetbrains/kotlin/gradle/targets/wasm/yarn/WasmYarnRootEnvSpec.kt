/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.yarn

import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootEnvSpec

/**
 * Spec for Yarn - package manager to install NPM dependencies
 */
abstract class WasmYarnRootEnvSpec internal constructor() : BaseYarnRootEnvSpec() {
    companion object : HasPlatformDisambiguate by WasmPlatformDisambiguate {
        val YARN: String
            get() = extensionName("kotlinYarnSpec")
    }

}