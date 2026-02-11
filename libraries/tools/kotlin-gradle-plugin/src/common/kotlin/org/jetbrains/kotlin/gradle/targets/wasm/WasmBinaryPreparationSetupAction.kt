/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.WasmBinary
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryAttribute
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.gradle.utils.setInvisibleIfSupported

@OptIn(ExperimentalWasmDsl::class)
internal val WasmBinaryPreparationSetupAction = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget || target.wasmTargetType != KotlinWasmTargetType.JS) return@KotlinTargetSideEffect

    val project = target.project
    if (!project.kotlinPropertiesProvider.wasmPerModule) return@KotlinTargetSideEffect

    val main = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    target.compilations.all { compilation ->

        val runtimeDependencyConfiguration = compilation.configurations.runtimeDependencyConfiguration

        compilation.binaries.configureEach { binary ->
            if (binary !is WasmBinary) return@configureEach

            val attributeValue = when (binary.mode) {
                KotlinJsBinaryMode.PRODUCTION -> WasmBinaryAttribute.WASM_BINARY_PRODUCTION
                KotlinJsBinaryMode.DEVELOPMENT -> WasmBinaryAttribute.WASM_BINARY_DEVELOPMENT
            }
            val wasmBinaryConfiguration = compilation.project.configurations.maybeCreateResolvable(binary.wasmBinaryConfigurationName) {
                description = "Wasm binaries for main."
                setInvisibleIfSupported()
                KotlinUsages.configureProducerRuntimeUsage(this, target)
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                attributes.attribute(WasmBinaryAttribute.attribute, attributeValue)

                usesPlatformOf(target)
            }

            wasmBinaryConfiguration.extendsFrom(runtimeDependencyConfiguration)
            if (compilation.isMain()) {

                val wasmBinaryOutputConfiguration = project.configurations.maybeCreateConsumable(binary.wasmBinaryOutputConfigurationName) {
                    description = "Wasm binary output."
                    setInvisibleIfSupported()
                    KotlinUsages.configureProducerRuntimeUsage(this, target)
                    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                    attributes.attribute(WasmBinaryAttribute.attribute, attributeValue)
                    usesPlatformOf(target)

                    project.artifacts.add(
                        binary.wasmBinaryOutputConfigurationName,
                        when (binary.mode) {
                            KotlinJsBinaryMode.PRODUCTION -> binary.optimizeTask.map { it.outputDirectory }
                            KotlinJsBinaryMode.DEVELOPMENT -> binary.linkTask.map { it.destinationDirectory }
                        }

                    )
                }

                wasmBinaryOutputConfiguration.extendsFrom(runtimeDependencyConfiguration)

            }

            if (compilation.isTest()) {
                binary.linkSyncTask.configure {
                    val mainBinary = main.binaries.getIrBinaries(binary.mode).single()
                    it.from.from(mainBinary.linkTask.map { it.destinationDirectory })
                }
            }
        }

        compilation.binaries.executableIrInternal(compilation)
    }
}