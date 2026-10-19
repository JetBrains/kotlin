/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
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
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.createDependencyScope
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.getAttributeSafely
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.setInvisibleIfSupported

internal val HasPlatformDisambiguator.npmSharedPackageJsonFilesForRootConfigurationName: String
    get() = extensionName("npmSharedPackageJsonFilesForRoot")

internal val HasPlatformDisambiguator.npmSharedDependenciesConfigurationName: String
    get() = extensionName("npmSharedDependencies")

internal val HasPlatformDisambiguator.npmSharedPackageJsonFilesFromSubprojectsResolverConfigurationName: String
    get() = extensionName("npmSharedPackageJsonFilesFromSubprojectsResolver")

internal val KotlinJsIrTarget.npmSharedPackageJsonForDependentsConfigurationName: String
    get() = lowerCamelCaseName(disambiguationClassifier, "npmSharedPackageJsonForDependents")

internal val KotlinJsIrCompilation.npmSharedPackageJsonFromDependenciesResolverConfigurationName: String
    get() = disambiguateName("npmSharedPackageJsonFromDependenciesResolver")

internal val HasPlatformDisambiguator.npmSharedPackageJsonFilesForRootUsageName: String
    get() = extensionName("npmSharedPackageJsonFilesForRoot")

internal val HasPlatformDisambiguator.npmSharedPackageJsonForDependentsUsageName: String
    get() = extensionName("npmSharedPackageJsonForDependents")

private fun Project.sharedPackageJsonAttributes(attributes: AttributeContainer, usageName: String) {
    attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(usageName))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
}

internal fun Project.maybeCreatePackageJsonsForRoot(platform: HasPlatformDisambiguator): Configuration =
    configurations.maybeCreateConsumable(platform.npmSharedPackageJsonFilesForRootConfigurationName) {
        setInvisibleIfSupported()
        description = "Kotlin package.json files of this project for the shared-npm-project."
        sharedPackageJsonAttributes(attributes, platform.npmSharedPackageJsonFilesForRootUsageName)
    }

internal fun Project.createPackageJsonsFromSubprojectsResolver(platform: HasPlatformDisambiguator): Configuration {
    val sharedDependencies = configurations.createDependencyScope(platform.npmSharedDependenciesConfigurationName) {
        setInvisibleIfSupported()
        description = "Projects contributing to the shared-npm-project."
    }

    return configurations.createResolvable(platform.npmSharedPackageJsonFilesFromSubprojectsResolverConfigurationName) {
        setInvisibleIfSupported()
        description = "Resolves the '${platform.npmSharedPackageJsonFilesForRootConfigurationName}' package.json files of the projects in '${platform.npmSharedDependenciesConfigurationName}'."
        extendsFrom(sharedDependencies.get())
        sharedPackageJsonAttributes(attributes, platform.npmSharedPackageJsonFilesForRootUsageName)
    }
}

internal fun Project.maybeCreatePackageJsonForDependents(
    target: KotlinJsIrTarget,
    platform: HasPlatformDisambiguator,
): Configuration =
    configurations.maybeCreateConsumable(target.npmSharedPackageJsonForDependentsConfigurationName) {
        setInvisibleIfSupported()
        description = "Shared package.json of the '${target.name}' target for dependent projects."
        sharedPackageJsonAttributes(attributes, platform.npmSharedPackageJsonForDependentsUsageName)
    }

internal fun Project.createPackageJsonsFromDependenciesResolver(compilation: KotlinJsIrCompilation): Configuration {
    val runtimeDependencies = declaredRuntimeDependencies(compilation)
    return configurations.createResolvable(compilation.npmSharedPackageJsonFromDependenciesResolverConfigurationName) {
        setInvisibleIfSupported()
        description = "Resolves the runtime dependencies of $compilation to collect their npm dependencies."
        usesPlatformOf(compilation.target)
        attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(compilation.target))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
        dependencies.addAllLater(runtimeDependencies)
    }
}

private fun Project.declaredRuntimeDependencies(compilation: KotlinJsIrCompilation): Provider<List<Dependency>> = provider {
    listOf(API_SCOPE, IMPLEMENTATION_SCOPE, RUNTIME_ONLY_SCOPE)
        .flatMap { scope -> compilationDependencyConfigurationByScope(compilation, scope).allDependencies }
        .filter { it !is FileCollectionDependency }
}

internal fun Project.sharedPackageJsonArtifacts(resolver: Configuration, platform: HasPlatformDisambiguator): FileCollection {
    val projectView = resolver.incoming.artifactView { view ->
        view.withVariantReselection()
        view.componentFilter { it is ProjectComponentIdentifier }
        view.attributes { attributes ->
            sharedPackageJsonAttributes(attributes, platform.npmSharedPackageJsonForDependentsUsageName)
        }
        view.lenient(true)
    }
    val moduleView = resolver.incoming.artifactView { view ->
        view.componentFilter { it is ModuleComponentIdentifier }
        view.lenient(true)
    }
    return files(
        projectView.npmSharedPackageJsonForDependentsFiles(platform.npmSharedPackageJsonForDependentsUsageName),
        moduleView.files,
    )
}

/** Reselection also matches variants that declare no Usage at all, so drop everything that is not ours: KT-85517. */
private fun ArtifactView.npmSharedPackageJsonForDependentsFiles(usageName: String) = artifacts.resolvedArtifacts
    .map { artifacts -> artifacts.filter { it.variant.attributes.getAttributeSafely(Usage.USAGE_ATTRIBUTE) == usageName }.map { it.file } }
