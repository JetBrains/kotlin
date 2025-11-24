/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryAttribute
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryTransform.Companion.ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable

internal val WasmBinaryPreparationSetupAction = KotlinCompilationSideEffect { compilation ->
    val target = compilation.target
    val project = target.project
    if (target !is KotlinJsIrTarget || target.wasmTargetType != KotlinWasmTargetType.JS)
        if (compilation !is KotlinJsIrCompilation) return@KotlinCompilationSideEffect
    compilation as KotlinJsIrCompilation

    val runtime = project.configurations.maybeCreateConsumable(target.runtimeElementsConfigurationName)

    project.dependencies.artifactTypes.maybeCreate(ARTIFACT_TYPE)

//    project.dependencies.attributesSchema.attribute(WasmBinaryAttribute.attribute) {
//        it.compatibilityRules.add(JarToWasmBinaryRule::class.java)
//    }

    project.configurations.maybeCreateResolvable(compilation.wasmBinaryConfigurationName) {
        description = "Elements of runtime for main."
        @Suppress("DEPRECATION")
        isVisible = false
        KotlinUsages.configureProducerRuntimeUsage(this, target)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(WasmBinaryAttribute.attribute, WasmBinaryAttribute.WASM_BINARY)
        val runtimeConfiguration = compilation.internal.configurations.deprecatedRuntimeConfiguration
        extendsFrom(runtime)
        runtimeConfiguration?.let { extendsFrom(it) }
        usesPlatformOf(target)
    }
}