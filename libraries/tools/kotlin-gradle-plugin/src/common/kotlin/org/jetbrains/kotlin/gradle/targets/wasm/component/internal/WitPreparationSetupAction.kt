/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.wasm.component.WIT_DIRECTORY_NAME

/**
 * Exposes the `wit` directory of the project as a secondary variant of its runtime elements,
 * so that consumers can request WIT declarations of a project dependency
 * by the [KOTLIN_WASM_WIT_ARTIFACT_TYPE] artifact type.
 *
 * WIT declarations of published libraries are extracted from their klibs by [WitExtractionTransform],
 * but klibs of project dependencies do not contain the `wit` directory of the producing project,
 * so it has to be exposed as a separate artifact.
 */
internal val WitPreparationSetupAction = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget || target.wasmTargetType != KotlinWasmTargetType.WASI) return@KotlinTargetSideEffect

    val project = target.project

    project.configurations.getByName(target.runtimeElementsConfigurationName)
        .outgoing
        .variants
        .maybeCreate(WIT_SECONDARY_VARIANT_NAME)
        .artifact(project.layout.projectDirectory.dir(WIT_DIRECTORY_NAME)) {
            it.type = KOTLIN_WASM_WIT_ARTIFACT_TYPE
        }
}

/**
 * Name of the secondary variant with WIT declarations of a project.
 */
internal const val WIT_SECONDARY_VARIANT_NAME = "wit"
