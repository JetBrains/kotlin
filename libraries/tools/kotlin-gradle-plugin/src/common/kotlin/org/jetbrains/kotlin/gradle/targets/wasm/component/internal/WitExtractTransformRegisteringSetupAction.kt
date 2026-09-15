/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.registerTransformForArtifactType

internal val WitExtractTransformRegisteringSetupAction = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget || target.wasmTargetType != KotlinWasmTargetType.WASI) return@KotlinTargetSideEffect

    val project = target.project

    project.dependencies.registerTransformForArtifactType(
        WitExtractionTransform::class.java,
        fromArtifactType = KLIB_TYPE,
        toArtifactType = WIT,
    )
}
