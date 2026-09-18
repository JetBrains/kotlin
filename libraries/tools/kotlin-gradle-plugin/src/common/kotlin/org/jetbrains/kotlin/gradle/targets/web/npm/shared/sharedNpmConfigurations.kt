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
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.createDependencyScope
import org.jetbrains.kotlin.gradle.utils.createResolvable
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

/** Offered to dependent projects: the package.json of the main compilation only, read by dependent projects. */
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
