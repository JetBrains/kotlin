/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin.Companion.kotlinNodeJsEnvSpec as wasmKotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

abstract class KotlinJsIrNpmBasedSubTarget internal constructor(
    target: KotlinJsIrTarget,
    disambiguationClassifier: String,
    createdWithPublicConstructor: ObjectFactory?,
) : KotlinJsIrSubTarget(target, disambiguationClassifier, createdWithPublicConstructor) {

    @Deprecated("Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.", level = DeprecationLevel.ERROR)
    constructor(
        target: KotlinJsIrTarget,
        disambiguationClassifier: String,
    ) : this(target, disambiguationClassifier, null)

    protected val nodeJsRoot = target.webTargetVariant(
        { project.rootProject.kotlinNodeJsRootExtension },
        { project.rootProject.wasmKotlinNodeJsRootExtension },
    )

    protected val nodeJsEnvSpec = target.webTargetVariant(
        { project.kotlinNodeJsEnvSpec },
        { project.wasmKotlinNodeJsEnvSpec },
    )
}
