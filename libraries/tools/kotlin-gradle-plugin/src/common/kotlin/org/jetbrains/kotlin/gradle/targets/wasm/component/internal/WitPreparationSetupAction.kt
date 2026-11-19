/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.API_SCOPE
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.IMPLEMENTATION_SCOPE
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.RUNTIME_ONLY_SCOPE
import org.jetbrains.kotlin.gradle.plugin.sources.compilationDependencyConfigurationByScope
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
 * Neither of them declares the artifact type, it is requested only by the artifact view of [witDirectories].
 */
internal val WitPreparationSetupAction = KotlinCompilationSideEffect { compilation ->
    if (compilation !is KotlinJsIrCompilation) return@KotlinCompilationSideEffect

    val target = compilation.target

    if (target.wasmTargetType != KotlinWasmTargetType.WASI) return@KotlinCompilationSideEffect

    createWitResolvableConfiguration(compilation)

    if (compilation.isMain()) {
        createWitOutputConsumableConfiguration(compilation)
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
    project.configurations.getByName(witConfigurationName).incoming
        .artifactView { view ->
            view.attributes {
                it.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_WASM_WIT_ARTIFACT_TYPE)
            }
        }.files

private fun Project.extendFromRuntimeDependencyScopes(
    configuration: Configuration,
    compilation: KotlinJsIrCompilation,
) {
    listOf(API_SCOPE, IMPLEMENTATION_SCOPE, RUNTIME_ONLY_SCOPE).forEach { scope ->
        // TODO(KT-91337): for associated compilations, this holds main output and triggers main compile.
        configuration.extendsFrom(compilationDependencyConfigurationByScope(compilation, scope))
    }
}

private fun createWitResolvableConfiguration(compilation: KotlinJsIrCompilation) {
    val target = compilation.target
    val project = target.project

    val witConfiguration = project.configurations.maybeCreateResolvable(compilation.witConfigurationName) {
        description = "Resolves WIT declarations."
        setInvisibleIfSupported()
        attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_WIT_RUNTIME_DEPENDENCIES))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))

        usesPlatformOf(target)
    }

    project.extendFromRuntimeDependencyScopes(witConfiguration, compilation)
}

private fun createWitOutputConsumableConfiguration(compilation: KotlinJsIrCompilation) {
    val target = compilation.target
    val project = target.project

    val witOutputConfiguration = project.configurations.maybeCreateConsumable(compilation.witOutputConfigurationName) {
        description = "Consumable configuration with WIT declarations."
        setInvisibleIfSupported()
        attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_WIT_RUNTIME_DEPENDENCIES))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        usesPlatformOf(target)

        outgoing.artifact(project.layout.projectDirectory.dir(WIT_DIRECTORY_NAME)) {
            it.type = KOTLIN_WASM_WIT_ARTIFACT_TYPE
        }
    }

    project.extendFromRuntimeDependencyScopes(witOutputConfiguration, compilation)
}
