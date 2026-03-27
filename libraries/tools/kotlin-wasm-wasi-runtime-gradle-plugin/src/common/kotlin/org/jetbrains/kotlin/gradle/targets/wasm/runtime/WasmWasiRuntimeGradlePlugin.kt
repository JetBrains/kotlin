/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.runtime

import org.gradle.api.Plugin
import org.gradle.api.Project

class WasmWasiRuntimeGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // The dsl is available when this plugin is applied or via the module dependency.
    }
}
