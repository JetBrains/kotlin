/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.createDependencyScope
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.setInvisibleIfSupported

internal val sharedNpmPlatformAttribute: Attribute<String> =
    Attribute.of("org.jetbrains.kotlin.npm.platform", String::class.java)

internal const val NPM_DEPENDENCIES_REPORT_USAGE = "kotlin-npm-dependencies-report"

/** Subproject side: publishes this project's package.json files, consumed by the root's [createResolvableNpmDependenciesReportConfiguration]. */
internal fun Project.maybeCreateConsumableNpmDependenciesReportConfiguration(platform: SharedNpmPlatform): Configuration {
    registerSharedNpmAttributes()
    return configurations.maybeCreateConsumable(platform.reportConfigurationName) {
        setInvisibleIfSupported()
        description = "Kotlin package.json files of the '${platform.targetId}' target for the shared-npm-project."
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(NPM_DEPENDENCIES_REPORT_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
        attributes.attribute(sharedNpmPlatformAttribute, platform.targetId)
    }
}

/** Root side: consumes the package.json files published by [maybeCreateConsumableNpmDependenciesReportConfiguration] of the projects declared on the bucket. */
internal fun Project.createResolvableNpmDependenciesReportConfiguration(platform: SharedNpmPlatform): Configuration {
    registerSharedNpmAttributes()

    val sharedDependencies = configurations.createDependencyScope(platform.sharedDependenciesConsumableConfigurationName) {
        setInvisibleIfSupported()
        description = "Projects contributing to the shared-npm-project of the '${platform.targetId}' target."
    }

    return configurations.createResolvable(platform.sharedDependenciesResolvableConfigurationName) {
        setInvisibleIfSupported()
        description = "Resolves the package.json files contributed to the shared-npm-project of the '${platform.targetId}' target."
        extendsFrom(sharedDependencies.get())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, usageByName(NPM_DEPENDENCIES_REPORT_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
        attributes.attribute(sharedNpmPlatformAttribute, platform.targetId)
    }
}

private fun Project.registerSharedNpmAttributes() {
    dependencies.attributesSchema {
        it.attribute(sharedNpmPlatformAttribute)
    }
}
