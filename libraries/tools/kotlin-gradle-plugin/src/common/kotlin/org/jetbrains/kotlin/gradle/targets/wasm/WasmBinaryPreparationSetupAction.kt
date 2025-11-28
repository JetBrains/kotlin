/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryAttribute
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryModeAttribute
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryModeAttribute.attributeByMode
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable

internal val WasmBinaryPreparationSetupAction = KotlinTargetSideEffect { target ->
    target.compilations.all { compilation ->
//        val target = compilation.target
        val project = target.project
        if (target !is KotlinJsIrTarget || target.wasmTargetType != KotlinWasmTargetType.JS)
            if (compilation !is KotlinJsIrCompilation) return@all
        compilation as KotlinJsIrCompilation

        val runtime = project.configurations.maybeCreateConsumable(target.runtimeElementsConfigurationName)
        val runtimeConfiguration = compilation.internal.configurations.deprecatedRuntimeConfiguration

//    project.dependencies.artifactTypes.maybeCreate(ARTIFACT_TYPE)

//    project.dependencies.attributesSchema.attribute(WasmBinaryAttribute.attribute) {
//        it.compatibilityRules.add(JarToWasmBinaryRule::class.java)
//    }

//    project.configurations.maybeCreateResolvable(compilation.wasmBinaryConfigurationName) {
//        description = "Elements of runtime for main."
//        @Suppress("DEPRECATION")
//        isVisible = false
//        KotlinUsages.configureProducerRuntimeUsage(this, target)
//        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
//        attributes.attribute(WasmBinaryAttribute.attribute, WasmBinaryAttribute.WASM_BINARY)
//        extendsFrom(runtime)
//        runtimeConfiguration?.let { extendsFrom(it) }
//        usesPlatformOf(target)
//    }

        if (compilation.isMain()) {
            compilation.binaries.configureEach { binary ->
                val conf = compilation.project.configurations.maybeCreateResolvable(binary.wasmBinaryConfigurationName) {
                    description = "Elements of runtime for main."
                    @Suppress("DEPRECATION")
                    isVisible = false
                    KotlinUsages.configureProducerRuntimeUsage(this, target)
                    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                    attributes.attribute(WasmBinaryAttribute.attribute, WasmBinaryAttribute.WASM_BINARY_DEVELOPMENT)
//                attributes.attribute(WasmBinaryModeAttribute.attribute, binary.mode.attributeByMode())

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

//        project.configurations.maybeCreateConsumable(compilation.wasmBinaryOutputConfigurationName) {
//            description = "Elements of runtime for main."
//            @Suppress("DEPRECATION")
//            isVisible = false
//            KotlinUsages.configureProducerRuntimeUsage(this, target)
//            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
//            attributes.attribute(WasmBinaryAttribute.attribute, WasmBinaryAttribute.WASM_BINARY)
//            extendsFrom(runtime)
//            runtimeConfiguration?.let { extendsFrom(it) }
//            usesPlatformOf(target)
//        }

            compilation.binaries.executableIrInternal(compilation)
        }
    }
}