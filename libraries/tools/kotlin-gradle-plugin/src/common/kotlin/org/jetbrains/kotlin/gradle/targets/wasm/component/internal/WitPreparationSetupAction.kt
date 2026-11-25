/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.wasm.component.WIT_DIRECTORY_NAME

/**
 * Exposes the `wit` directory of the project as a secondary variant of its runtime elements.
 *
 * The variant has the dedicated [KotlinUsages.KOTLIN_WIT_RUNTIME_DEPENDENCIES] usage,
 * so it is never selected by consumers of the runtime usage, even if they don't request the artifact type.
 * Consumers request it by both this usage and the [KOTLIN_WASM_WIT_ARTIFACT_TYPE] artifact type, see [witDirectories].
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
        .apply {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_WIT_RUNTIME_DEPENDENCIES))
            artifact(project.layout.projectDirectory.dir(WIT_DIRECTORY_NAME)) {
                it.type = KOTLIN_WASM_WIT_ARTIFACT_TYPE
            }
        }
}

/**
 * WIT directories of the runtime dependencies of this compilation:
 * the `wit` directories of project dependencies and the WIT declarations extracted from klibs by [WitExtractionTransform].
 *
 * Dependencies without WIT declarations, such as Java libraries, are skipped,
 * because their artifacts can't be transformed to [KOTLIN_WASM_WIT_ARTIFACT_TYPE].
 */
internal fun KotlinJsIrCompilation.witDirectories(project: Project): FileCollection =
    project.configurations.getByName(runtimeDependencyConfigurationName).incoming
        .artifactView { view ->
            view.attributes {
                it.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_WIT_RUNTIME_DEPENDENCIES))
                it.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_WASM_WIT_ARTIFACT_TYPE)
            }
        }.files

/**
 * Name of the secondary variant with WIT declarations of a project.
 */
internal const val WIT_SECONDARY_VARIANT_NAME = "wit"
