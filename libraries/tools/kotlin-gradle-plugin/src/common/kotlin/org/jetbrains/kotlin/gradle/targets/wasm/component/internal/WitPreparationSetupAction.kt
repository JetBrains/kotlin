/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
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

    val wasmBinaryConfiguration = compilation.project.configurations.maybeCreateResolvable(compilation.witConfigurationName) {
        description = "Resolves wit declarations."
        setInvisibleIfSupported()
        KotlinUsages.configureProducerRuntimeUsage(this, target)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, WIT)

        usesPlatformOf(target)
    }

    wasmBinaryConfiguration.extendsFrom(runtimeDependencyConfiguration)
}

private fun createWitOutputConsumableConfiguration(
    compilation: KotlinJsIrCompilation,
    runtimeDependencyConfiguration: Configuration?,
) {
    val target = compilation.target
    val project = target.project

    val wasmBinaryOutputConfiguration = project.configurations.maybeCreateConsumable(compilation.witOutputConfigurationName) {
        description = "Consumable configuration with wit declarations."
        setInvisibleIfSupported()
        KotlinUsages.configureProducerRuntimeUsage(this, target)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, WIT)
        usesPlatformOf(target)

        project.artifacts.add(
            compilation.witOutputConfigurationName,
            project.layout.projectDirectory.dir("wit")

        )
    }

    wasmBinaryOutputConfiguration.extendsFrom(runtimeDependencyConfiguration)
}
