/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.API_SCOPE
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.IMPLEMENTATION_SCOPE
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.RUNTIME_ONLY_SCOPE
import org.jetbrains.kotlin.gradle.plugin.sources.compilationDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.createDependencyScope
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.registerArtifact
import org.jetbrains.kotlin.gradle.utils.setInvisibleIfSupported

internal val HasPlatformDisambiguator.platformName: String
    get() = platformDisambiguator ?: JsPlatformDisambiguator.jsPlatform

internal val HasPlatformDisambiguator.npmDependenciesReportConfigurationName: String
    get() = lowerCamelCaseName("kotlin", "npmDependenciesReport", platformName)

internal val HasPlatformDisambiguator.npmSharedDependenciesConfigurationName: String
    get() = extensionName("npmSharedDependencies")

internal val HasPlatformDisambiguator.npmDependenciesReportResolverConfigurationName: String
    get() = lowerCamelCaseName("kotlin", "npmDependenciesReport", platformName, "resolver")

internal val KotlinJsIrTarget.npmRuntimeDependenciesVariantName: String
    get() = lowerCamelCaseName("kotlin", "npmRuntimeDependencies", disambiguationClassifier)

internal val KotlinJsIrCompilation.npmRuntimeDependenciesResolverConfigurationName: String
    get() = disambiguateName("npmRuntimeDependenciesResolver")

internal val HasPlatformDisambiguator.npmDependenciesReportUsageName: String
    get() = npmDependenciesReportConfigurationName

internal val HasPlatformDisambiguator.npmRuntimeDependenciesUsageName: String
    get() = lowerCamelCaseName("kotlin", "npmRuntimeDependencies", platformName)

private fun Project.npmDependenciesReportAttributes(attributes: AttributeContainer, usageName: String) {
    attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(usageName))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
}

internal fun Project.maybeCreateNpmDependenciesReport(platform: HasPlatformDisambiguator): Configuration =
    configurations.maybeCreateConsumable(platform.npmDependenciesReportConfigurationName) {
        setInvisibleIfSupported()
        description = "npm dependency reports of this project for the shared-npm-project."
        npmDependenciesReportAttributes(attributes, platform.npmDependenciesReportUsageName)
    }

internal fun Project.createNpmDependenciesReportResolver(platform: HasPlatformDisambiguator): Configuration {
    val sharedDependencies = configurations.createDependencyScope(platform.npmSharedDependenciesConfigurationName) {
        setInvisibleIfSupported()
        description = "Projects contributing to the shared-npm-project."
    }

    return configurations.createResolvable(platform.npmDependenciesReportResolverConfigurationName) {
        setInvisibleIfSupported()
        description = "Resolves the '${platform.npmDependenciesReportConfigurationName}' npm dependency reports of the projects in '${platform.npmSharedDependenciesConfigurationName}'."
        extendsFrom(sharedDependencies.get())
        npmDependenciesReportAttributes(attributes, platform.npmDependenciesReportUsageName)
    }
}

internal const val NPM_DEPENDENCY_REPORT_ARTIFACT_TYPE = "kotlin-npm-dependency-report"

private const val JSON_TYPE = "json"

private const val FILE_DEPENDENCY_USAGE = "kotlinNpmFileDependency"

/** Offers the main compilation's report to projects that depend on this one, next to its klib. */
internal fun Project.createNpmRuntimeDependenciesVariant(
    target: KotlinJsIrTarget,
    platform: HasPlatformDisambiguator,
    report: Provider<RegularFile>,
) {
    val runtimeElements = configurations.getByName(target.runtimeElementsConfigurationName)
    runtimeElements.outgoing.variants.create(target.npmRuntimeDependenciesVariantName) { variant ->
        variant.attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(platform.npmRuntimeDependenciesUsageName))
        variant.registerArtifact(report, type = NPM_DEPENDENCY_REPORT_ARTIFACT_TYPE)
    }
}

internal fun Project.registerOncePackageJsonToNpmDependencyReportTransform(target: KotlinJsIrTarget, platform: HasPlatformDisambiguator) {
    val registeredKey = "PackageJsonToNpmDependencyReportTransform.${platform.platformName}"
    if (!extensions.extraProperties.has(registeredKey)) {
        registerPackageJsonToNpmDependencyReportTransform(target, platform)
        extensions.extraProperties[registeredKey] = true
    }
}

/** Covers Maven klibs and `files(...)` klibs, unpacked klibs and `package.json` files, which come without a report. */
private fun Project.registerPackageJsonToNpmDependencyReportTransform(target: KotlinJsIrTarget, platform: HasPlatformDisambiguator) {
    val fileDependencyUsage = usageByName(FILE_DEPENDENCY_USAGE)
    // KMP root variants have no artifactType, so they would match all three transforms.
    // Fake usage keeps them off the file transforms: `files(...)` deps have no usage, so still match.
    val sources = listOf(
        KLIB_TYPE to KotlinUsages.consumerRuntimeUsage(target),
        DIRECTORY_TYPE to fileDependencyUsage,
        JSON_TYPE to fileDependencyUsage,
    )
    for ((fromArtifactType, fromUsage) in sources) {
        dependencies.registerTransform(PackageJsonToNpmDependencyReportTransform::class.java) { transform ->
            transform.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, fromArtifactType)
            transform.from.attribute(Usage.USAGE_ATTRIBUTE, fromUsage)
            transform.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, NPM_DEPENDENCY_REPORT_ARTIFACT_TYPE)
            transform.to.attribute(Usage.USAGE_ATTRIBUTE, usageByName(platform.npmRuntimeDependenciesUsageName))
            transform.parameters.target.set(platform.platformName)
        }
    }
}

internal fun Project.createNpmRuntimeDependenciesResolver(compilation: KotlinJsIrCompilation): Configuration =
    configurations.createResolvable(compilation.npmRuntimeDependenciesResolverConfigurationName) {
        setInvisibleIfSupported()
        description = "Resolves the runtime dependencies of $compilation to collect their npm dependencies."
        usesPlatformOf(compilation.target)
        attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(compilation.target))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
        listOf(API_SCOPE, IMPLEMENTATION_SCOPE, RUNTIME_ONLY_SCOPE).forEach { scope ->
            extendsFrom(compilationDependencyConfigurationByScope(compilation, scope))
        }
    }

/** Projects offer their report; other dependencies are turned into one by [PackageJsonToNpmDependencyReportTransform]. */
internal fun Project.npmRuntimeDependenciesReports(resolver: Configuration, platform: HasPlatformDisambiguator): FileCollection =
    resolver.incoming.artifactView { view ->
        view.attributes { attributes ->
            attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(platform.npmRuntimeDependenciesUsageName))
            attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, NPM_DEPENDENCY_REPORT_ARTIFACT_TYPE)
        }
    }.files
