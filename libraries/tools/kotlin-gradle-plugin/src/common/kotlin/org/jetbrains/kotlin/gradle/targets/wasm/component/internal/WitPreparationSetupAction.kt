/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.wasm.component.WIT_DIRECTORY_NAME
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.gradle.utils.setInvisibleIfSupported

/**
 * Sets up the configurations that share WIT declarations between Wasm WASI projects:
 *
 * - a resolvable [KotlinJsIrCompilation.witConfigurationName] configuration for each compilation.
 *   It resolves WIT directories of the runtime dependencies for [org.jetbrains.kotlin.gradle.targets.wasm.component.WasmComponentizeTask].
 *   WIT declarations of published libraries are extracted from their klibs by [WitExtractionTransform].
 * - a consumable [KotlinJsIrCompilation.witOutputConfigurationName] configuration for the main compilation.
 *   It exposes the `wit` directory of the project as is, because klibs of project dependencies
 *   do not contain the `wit` directory of the producing project.
 *
 * Both configurations use the dedicated [KotlinUsages.KOTLIN_WIT_RUNTIME_DEPENDENCIES] usage,
 * so the consumable one is not selected for regular runtime resolution instead of the runtime elements of the project,
 * while the resolvable one still falls back to the klibs of dependencies without WIT declarations variant.
 */
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
        attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_WIT_RUNTIME_DEPENDENCIES))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))

        usesPlatformOf(target)
    }

    runtimeDependencyConfiguration?.let { witConfiguration.extendsFrom(it) }
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
        attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_WIT_RUNTIME_DEPENDENCIES))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_WASM_WIT_ARTIFACT_TYPE)
        usesPlatformOf(target)

        project.artifacts.add(
            compilation.witOutputConfigurationName,
            project.layout.projectDirectory.dir(WIT_DIRECTORY_NAME)
        )
    }

    runtimeDependencyConfiguration?.let { witOutputConfiguration.extendsFrom(it) }
}
