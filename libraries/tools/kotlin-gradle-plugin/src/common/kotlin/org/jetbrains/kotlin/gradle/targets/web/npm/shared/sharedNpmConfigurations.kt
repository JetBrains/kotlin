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

internal val HasPlatformDisambiguator.npmSharedPackageJsonFilesConfigurationName: String
    get() = extensionName("npmSharedPackageJsonFiles")

internal val HasPlatformDisambiguator.npmSharedDependenciesConfigurationName: String
    get() = extensionName("npmSharedDependencies")

internal val HasPlatformDisambiguator.npmSharedDependenciesResolverConfigurationName: String
    get() = extensionName("npmSharedDependenciesResolver")

/** Per target: `jsSharedPackageJsonElements`, `wasmJsSharedPackageJsonElements`. */
internal val KotlinJsIrTarget.sharedPackageJsonElementsConfigurationName: String
    get() = lowerCamelCaseName(disambiguationClassifier, "sharedPackageJsonElements")

/** Per compilation: `jsMainSharedPackageJsonResolver`, `jsTestSharedPackageJsonResolver`, ... */
internal val KotlinJsIrCompilation.sharedPackageJsonResolverConfigurationName: String
    get() = disambiguateName("sharedPackageJsonResolver")

/** The platform is part of the name, so js and wasm are two unrelated usages and need no platform attribute. */
internal val HasPlatformDisambiguator.sharedPackageJsonUsageName: String
    get() = extensionName("npmSharedPackageJsonElements")

/** Subproject side: publishes this project's package.json files, consumed by the root's [createSubprojectPackageJsonsResolver]. */
internal fun Project.maybeCreatePackageJsonsForRootProject(platform: HasPlatformDisambiguator): Configuration =
    configurations.maybeCreateConsumable(platform.npmSharedPackageJsonFilesConfigurationName) {
        setInvisibleIfSupported()
        description = "Kotlin package.json files of this project for the shared-npm-project."
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(platform.npmSharedPackageJsonFilesConfigurationName))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
    }

/** Root side: consumes the package.json files published by [maybeCreatePackageJsonsForRootProject] of the projects declared on the bucket. */
internal fun Project.createSubprojectPackageJsonsResolver(platform: HasPlatformDisambiguator): Configuration {
    val sharedDependencies = configurations.createDependencyScope(platform.npmSharedDependenciesConfigurationName) {
        setInvisibleIfSupported()
        description = "Projects contributing to the shared-npm-project."
    }

    return configurations.createResolvable(platform.npmSharedDependenciesResolverConfigurationName) {
        setInvisibleIfSupported()
        description = "Resolves the '${platform.npmSharedPackageJsonFilesConfigurationName}' package.json files of the projects in '${platform.npmSharedDependenciesConfigurationName}'."
        extendsFrom(sharedDependencies.get())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(platform.npmSharedPackageJsonFilesConfigurationName))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
    }
}

/** Offered to dependent projects: the package.json of the main compilation only, read by [sharedPackageJsonArtifacts]. */
internal fun Project.maybeCreatePackageJsonForDependentProjects(
    target: KotlinJsIrTarget,
    platform: HasPlatformDisambiguator,
): Configuration =
    configurations.maybeCreateConsumable(target.sharedPackageJsonElementsConfigurationName) {
        setInvisibleIfSupported()
        description = "Shared package.json of the '${target.name}' target for dependent projects."
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(platform.sharedPackageJsonUsageName))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
    }

/** Compilation side: resolves the compilation's runtime graph; its artifacts are read by [sharedPackageJsonArtifacts]. */
internal fun Project.createResolvableSharedPackageJsonConfiguration(compilation: KotlinJsIrCompilation): Configuration =
    configurations.createResolvable(compilation.sharedPackageJsonResolverConfigurationName) {
        setInvisibleIfSupported()
        description = "Resolves the runtime dependencies of $compilation to collect their npm dependencies."
        usesPlatformOf(compilation.target)
        attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(compilation.target))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
        dependencies.addAllLater(this@createResolvableSharedPackageJsonConfiguration.declaredRuntimeDependencies(compilation))
    }

/**
 * Runtime scopes of [compilation]; compileOnly is not valid for non-JVM. The source sets' own dependencies are included:
 * `KotlinCompilationSourceSetInclusion` makes these configurations extend those of every source set of the compilation.
 */
private fun Project.declaredRuntimeDependencies(compilation: KotlinJsIrCompilation): Provider<List<Dependency>> = provider {
    listOf(API_SCOPE, IMPLEMENTATION_SCOPE, RUNTIME_ONLY_SCOPE)
        .flatMap { scope -> compilationDependencyConfigurationByScope(compilation, scope).allDependencies }
        .filter { it !is FileCollectionDependency }
}

/** Transitive npm dependencies of [resolver]'s graph: dependency projects' package.json, Maven modules' klibs. */
internal fun Project.sharedPackageJsonArtifacts(resolver: Configuration, platform: HasPlatformDisambiguator): FileCollection {
    val projectView = resolver.incoming.artifactView { view ->
        view.withVariantReselection()
        view.componentFilter { it is ProjectComponentIdentifier }
        view.attributes { attributes ->
            attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(platform.sharedPackageJsonUsageName))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
        }
        view.lenient(true)
    }
    val moduleView = resolver.incoming.artifactView { view ->
        view.componentFilter { it is ModuleComponentIdentifier }
        view.lenient(true)
    }
    return files(
        projectView.sharedPackageJsonElementsFiles(platform.sharedPackageJsonUsageName),
        moduleView.files,
    )
}

/** Reselection also matches variants that declare no Usage at all, so drop everything that is not ours: KT-85517. */
private fun ArtifactView.sharedPackageJsonElementsFiles(usageName: String) = artifacts.resolvedArtifacts
    .map { artifacts -> artifacts.filter { it.variant.attributes.getAttributeSafely(Usage.USAGE_ATTRIBUTE) == usageName }.map { it.file } }
