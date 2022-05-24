/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName

abstract class GradleKpmPublishedVariantWithRuntime(
    containingModule: GradleKpmModule, fragmentName: String,
    dependencyConfigurations: GradleKpmFragmentDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    runtimeDependencyConfiguration: Configuration,
    runtimeElementsConfiguration: Configuration
) : GradleKpmVariantWithRuntimeInternal(
    containingModule = containingModule,
    fragmentName = fragmentName,
    dependencyConfigurations = dependencyConfigurations,
    compileDependencyConfiguration = compileDependencyConfiguration,
    apiElementsConfiguration = apiElementsConfiguration,
    runtimeDependenciesConfiguration = runtimeDependencyConfiguration,
    runtimeElementsConfiguration = runtimeElementsConfiguration
), GradleKpmSingleMavenPublishedModuleHolder by GradleKpmDefaultSingleMavenPublishedModuleHolder(
    containingModule, defaultModuleSuffix(containingModule, fragmentName)
) {
    override val gradleVariantNames: Set<String>
        get() = listOf(apiElementsConfiguration.name, runtimeElementsConfiguration.name).flatMapTo(mutableSetOf()) {
            listOf(it, publishedConfigurationName(it))
        }
}

private fun defaultModuleSuffix(module: GradleKpmModule, variantName: String): String =
    dashSeparatedName(variantName, module.moduleClassifier)
