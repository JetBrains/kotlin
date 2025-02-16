/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

@Deprecated("Use BinaryenPlugin instead", ReplaceWith("BinaryenPlugin"))
@OptIn(ExperimentalWasmDsl::class)
open class BinaryenRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(BinaryenPlugin::class.java)
    }
}
