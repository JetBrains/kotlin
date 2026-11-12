/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.*
import org.jetbrains.kotlin.gradle.plugin.sources.compilationDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.web.KotlinWebPlatformType
import org.jetbrains.kotlin.gradle.utils.*

internal val KotlinWebPlatformType.npmDependenciesReportConfigurationName: String
    get() = lowerCamelCaseName("kotlin", "npmDependenciesReport", platformName)

internal val KotlinWebPlatformType.npmSharedDependenciesConfigurationName: String
    get() = lowerCamelCaseName("kotlin", "npmSharedDependencies", platformName)

internal val KotlinWebPlatformType.npmDependenciesReportResolverConfigurationName: String
    get() = lowerCamelCaseName(npmDependenciesReportConfigurationName, "resolver")

internal val KotlinJsIrTarget.npmRuntimeDependenciesConfigurationName: String
    get() = lowerCamelCaseName("kotlin", "npmRuntimeDependencies", disambiguationClassifier)

internal val KotlinJsIrCompilation.npmRuntimeDependenciesResolverConfigurationName: String
    get() = disambiguateName("npmRuntimeDependenciesResolver")

private const val NPM_DEPENDENCIES_REPORT_USAGE = "kotlinNpmDependenciesReport"

/** Sets the attributes that tell the JS and the WasmJS npm dependency reports apart. */
private fun AttributeContainer.platformAttributes(platform: KotlinWebPlatformType) {
    when (platform) {
        KotlinWebPlatformType.JS -> attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        KotlinWebPlatformType.WasmJS -> {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
            attribute(KotlinWasmTargetAttribute.wasmTargetAttribute, KotlinWasmTargetAttribute.js)
        }
    }
}

private fun Project.npmDependenciesReportAttributes(attributes: AttributeContainer, platform: KotlinWebPlatformType) {
    attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(NPM_DEPENDENCIES_REPORT_USAGE))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
    attributes.platformAttributes(platform)
}

internal fun Project.maybeCreateNpmDependenciesReport(platform: KotlinWebPlatformType): Configuration =
    configurations.maybeCreateConsumable(platform.npmDependenciesReportConfigurationName) {
        setInvisibleIfSupported()
        description = "npm dependency reports of this project for the shared-npm-project."
        npmDependenciesReportAttributes(attributes, platform)
    }

internal fun Project.createNpmDependenciesReportResolver(platform: KotlinWebPlatformType): Configuration {
    val sharedDependencies = configurations.createDependencyScope(platform.npmSharedDependenciesConfigurationName) {
        setInvisibleIfSupported()
        description = "Projects contributing to the shared-npm-project."
    }

    return configurations.createResolvable(platform.npmDependenciesReportResolverConfigurationName) {
        setInvisibleIfSupported()
        description =
            "Resolves the '${platform.npmDependenciesReportConfigurationName}' npm dependency reports of the projects in '${platform.npmSharedDependenciesConfigurationName}'."
        extendsFrom(sharedDependencies.get())
        npmDependenciesReportAttributes(attributes, platform)
    }
}

/**
 * Offers [report] as the npm dependency report of [target] to projects that depend on this one.
 *
 * Not a variant of `runtimeElements`: it would inherit the klib's attributes, and consumers selecting `runtimeElements`
 * by name could not tell the report from the klib.
 */
internal fun Project.createNpmRuntimeDependenciesElements(
    target: KotlinJsIrTarget,
    report: Provider<RegularFile>,
) {
    configurations.createConsumable(target.npmRuntimeDependenciesConfigurationName) {
        setInvisibleIfSupported()
        description = "npm dependency report of $target, for the projects that depend on it."
        usesPlatformOf(target)
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(KotlinUsages.KOTLIN_NPM_RUNTIME_DEPENDENCIES))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
        outgoing.artifact(report)
    }
}

/**
 * Resolves the npm dependency report of project dependencies,
 * and falls back to the klib of other dependencies, see [KotlinUsages.KOTLIN_NPM_RUNTIME_DEPENDENCIES].
 */
internal fun Project.createNpmRuntimeDependenciesResolver(compilation: KotlinJsIrCompilation): Configuration =
    configurations.createResolvable(compilation.npmRuntimeDependenciesResolverConfigurationName) {
        setInvisibleIfSupported()
        description = "Resolves the runtime dependencies of $compilation to collect their npm dependencies."
        usesPlatformOf(compilation.target)
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(KotlinUsages.KOTLIN_NPM_RUNTIME_DEPENDENCIES))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
        listOf(API_SCOPE, IMPLEMENTATION_SCOPE, RUNTIME_ONLY_SCOPE).forEach { scope ->
            // TODO(KT-91337): for associated compilations, this holds main output and triggers main compile.
            extendsFrom(compilationDependencyConfigurationByScope(compilation, scope))
        }
    }
