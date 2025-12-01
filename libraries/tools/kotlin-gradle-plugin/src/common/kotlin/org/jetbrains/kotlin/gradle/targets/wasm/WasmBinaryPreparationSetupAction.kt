/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.WasmBinary
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryAttribute
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable

@OptIn(ExperimentalWasmDsl::class)
internal val WasmBinaryPreparationSetupAction = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget || target.wasmTargetType != KotlinWasmTargetType.JS) return@KotlinTargetSideEffect

    val project = target.project
    if (!project.kotlinPropertiesProvider.wasmPerModule) return@KotlinTargetSideEffect

    target.compilations.all { compilation ->

        val runtime = project.configurations.getByName(target.runtimeElementsConfigurationName)
        val runtimeConfiguration = compilation.internal.configurations.deprecatedRuntimeConfiguration

        if (compilation.isMain()) {
            compilation.binaries.configureEach { binary ->
                if (binary !is WasmBinary) return@configureEach
                val conf = compilation.project.configurations.maybeCreateResolvable(binary.wasmBinaryConfigurationName) {
                    description = "Elements of runtime for main."
                    @Suppress("DEPRECATION")
                    isVisible = false
                    KotlinUsages.configureProducerRuntimeUsage(this, target)
                    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                    attributes.attribute(WasmBinaryAttribute.attribute, WasmBinaryAttribute.WASM_BINARY_DEVELOPMENT)

                    usesPlatformOf(target)
                }

                conf.extendsFrom(runtime)
                runtimeConfiguration?.let { conf.extendsFrom(it) }

                if (binary.mode == KotlinJsBinaryMode.DEVELOPMENT) {
                    val conf2 = project.configurations.maybeCreateConsumable(binary.wasmBinaryOutputConfigurationName) {
                        description = "Elements of runtime for main."
                        @Suppress("DEPRECATION")
                        isVisible = false
                        KotlinUsages.configureProducerRuntimeUsage(this, target)
                        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                        attributes.attribute(WasmBinaryAttribute.attribute, WasmBinaryAttribute.WASM_BINARY_DEVELOPMENT)
                        usesPlatformOf(target)

                        project.artifacts.add(binary.wasmBinaryOutputConfigurationName, binary.linkTask.map { it.destinationDirectory })
                    }

                    conf2.extendsFrom(runtime)
                    runtimeConfiguration?.let { conf2.extendsFrom(it) }
                }

            }

            compilation.binaries.executableIrInternal(compilation)
        }
    }
}