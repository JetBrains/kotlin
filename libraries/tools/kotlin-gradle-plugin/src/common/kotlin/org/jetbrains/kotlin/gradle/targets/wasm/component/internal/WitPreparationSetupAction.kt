/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.wasm.component.WIT_DIRECTORY_NAME
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.gradle.utils.setInvisibleIfSupported

internal val WitPreparationSetupAction = KotlinCompilationSideEffect { compilation ->
    if (compilation !is KotlinJsIrCompilation) return@KotlinCompilationSideEffect

    val target = compilation.target

    if (target.wasmTargetType != KotlinWasmTargetType.WASI) return@KotlinCompilationSideEffect

    val runtimeDependencyConfiguration = compilation.configurations.runtimeDependencyConfiguration

    createWitResolvableConfiguration(
        compilation,
        runtimeDependencyConfiguration,
    )

    if (compilation.isMain()) {
        createWitOutputConsumableConfiguration(
            compilation,
            runtimeDependencyConfiguration,
        )
    }
}

private fun createWitResolvableConfiguration(
    compilation: KotlinJsIrCompilation,
    runtimeDependencyConfiguration: Configuration?,
) {
    val target = compilation.target
    val project = target.project

    val witConfiguration = compilation.project.configurations.maybeCreateResolvable(compilation.witConfigurationName) {
        description = "Resolves WIT declarations."
        setInvisibleIfSupported()
        KotlinUsages.configureProducerRuntimeUsage(this, target)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_WASM_WIT_ARTIFACT_TYPE)

        usesPlatformOf(target)
    }

    witConfiguration.extendsFrom(runtimeDependencyConfiguration)
}

private fun createWitOutputConsumableConfiguration(
    compilation: KotlinJsIrCompilation,
    runtimeDependencyConfiguration: Configuration?,
) {
    val target = compilation.target
    val project = target.project

    val witOutputConfiguration = project.configurations.maybeCreateConsumable(compilation.witOutputConfigurationName) {
        description = "Consumable configuration with WIT declarations."
        setInvisibleIfSupported()
        KotlinUsages.configureProducerRuntimeUsage(this, target)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_WASM_WIT_ARTIFACT_TYPE)
        usesPlatformOf(target)

        project.artifacts.add(
            compilation.witOutputConfigurationName,
            project.layout.projectDirectory.dir(WIT_DIRECTORY_NAME)
        )
    }

    witOutputConfiguration.extendsFrom(runtimeDependencyConfiguration)
}
