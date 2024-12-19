/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.targetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin.Companion.kotlinNodeJsEnvSpec as wasmKotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

abstract class KotlinJsIrNpmBasedSubTarget(
    target: KotlinJsIrTarget,
    disambiguationClassifier: String,
) : KotlinJsIrSubTarget(target, disambiguationClassifier) {
    protected val nodeJsRoot = target.targetVariant(
        { project.rootProject.kotlinNodeJsRootExtension },
        { project.rootProject.wasmKotlinNodeJsRootExtension },
    )

    protected val nodeJsEnvSpec = target.targetVariant(
        { project.kotlinNodeJsEnvSpec },
        { project.wasmKotlinNodeJsEnvSpec },
    )
}
