/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.legacyApiConfigurationName
import org.jetbrains.kotlin.gradle.plugin.mpp.legacyImplementationConfigurationName
import org.jetbrains.kotlin.gradle.plugin.mpp.legacyRuntimeOnlyConfigurationName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.createDependencyScope
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.setInvisibleIfSupported

internal val HasPlatformDisambiguator.npmSharedPackageJsonFilesConfigurationName: String
    get() = extensionName("npmSharedPackageJsonFiles")

internal val HasPlatformDisambiguator.npmSharedDependenciesConfigurationName: String
    get() = extensionName("npmSharedDependencies")

internal val HasPlatformDisambiguator.npmSharedDependenciesResolverConfigurationName: String
    get() = extensionName("npmSharedDependenciesResolver")

/** Subproject side: publishes this project's package.json files, consumed by the root's [createResolvableNpmSharedPackageJsonFilesConfiguration]. */
internal fun Project.maybeCreateConsumableNpmSharedPackageJsonFilesConfiguration(platform: HasPlatformDisambiguator): Configuration =
    configurations.maybeCreateConsumable(platform.npmSharedPackageJsonFilesConfigurationName) {
        setInvisibleIfSupported()
        description = "Kotlin package.json files of this project for the shared-npm-project."
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(platform.npmSharedPackageJsonFilesConfigurationName))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
    }

/**
 * Compilation side: consumes the package.json files published by
 * [maybeCreateConsumableNpmSharedPackageJsonFilesConfiguration] of the projects this compilation depends on at runtime.
 *
 * The npm dependencies of those package.json files are merged into the package.json of this compilation,
 * so that the npm dependencies declared by a project are visible to the projects depending on it.
 *
 * Only the runtime dependency scopes are inherited, compile-only dependencies are intentionally ignored,
 * which makes the configuration a npm counterpart of the runtime classpath of the compilation.
 */
internal fun createResolvableCompilationNpmPackageJsonFilesConfiguration(
    compilation: KotlinJsIrCompilation,
    platform: HasPlatformDisambiguator,
): Configuration {
    val project = compilation.project

    return project.configurations.createResolvable(compilation.disambiguateName("npmPackageJsonFilesResolver")) {
        setInvisibleIfSupported()
        description = "Resolves the '${platform.npmSharedPackageJsonFilesConfigurationName}' package.json files " +
                "of the runtime dependencies of the ${compilation.name} compilation."

        // The configurations of the compilation already aggregate the dependencies of all its source sets,
        // as well as the declared dependencies of the associated compilations.
        listOf(
            compilation.legacyApiConfigurationName,
            compilation.legacyImplementationConfigurationName,
            compilation.legacyRuntimeOnlyConfigurationName,
        ).forEach { configurationName ->
            project.configurations.findByName(configurationName)?.let { extendsFrom(it) }
        }

        attributes.attribute(
            Usage.USAGE_ATTRIBUTE,
            project.usageByName(platform.npmSharedPackageJsonFilesConfigurationName)
        )
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
    }
}

/** Root side: consumes the package.json files published by [maybeCreateConsumableNpmSharedPackageJsonFilesConfiguration] of the projects declared on the bucket. */
internal fun Project.createResolvableNpmSharedPackageJsonFilesConfiguration(platform: HasPlatformDisambiguator): Configuration {
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