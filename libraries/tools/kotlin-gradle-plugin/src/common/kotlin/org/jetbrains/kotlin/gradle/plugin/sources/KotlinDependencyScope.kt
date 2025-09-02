/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.legacyApiConfigurationName
import org.jetbrains.kotlin.gradle.plugin.mpp.legacyCompileOnlyConfigurationName
import org.jetbrains.kotlin.gradle.plugin.mpp.legacyImplementationConfigurationName
import org.jetbrains.kotlin.gradle.plugin.mpp.legacyRuntimeOnlyConfigurationName
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.*
import org.jetbrains.kotlin.gradle.utils.API
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY
import org.jetbrains.kotlin.gradle.utils.IMPLEMENTATION
import org.jetbrains.kotlin.gradle.utils.RUNTIME_ONLY

internal enum class KotlinDependencyScope(val scopeName: String) {
    API_SCOPE(API),
    IMPLEMENTATION_SCOPE(IMPLEMENTATION),
    COMPILE_ONLY_SCOPE(COMPILE_ONLY),
    RUNTIME_ONLY_SCOPE(RUNTIME_ONLY);

    companion object {
        val compileScopes = listOf(API_SCOPE, IMPLEMENTATION_SCOPE, COMPILE_ONLY_SCOPE)
    }
}

internal fun ConfigurationContainer.sourceSetDependencyConfigurationByScope(
    kotlinDependenciesContainer: HasKotlinDependencies,
    scope: KotlinDependencyScope
): Configuration = getByName(
    when (scope) {
        API_SCOPE -> kotlinDependenciesContainer.apiConfigurationName
        IMPLEMENTATION_SCOPE -> kotlinDependenciesContainer.implementationConfigurationName
        COMPILE_ONLY_SCOPE -> kotlinDependenciesContainer.compileOnlyConfigurationName
        RUNTIME_ONLY_SCOPE -> kotlinDependenciesContainer.runtimeOnlyConfigurationName
    }
)

internal fun Project.compilationDependencyConfigurationByScope(
    compilation: KotlinCompilation<*>,
    scope: KotlinDependencyScope
): Configuration =
    project.configurations.getByName(
        when (scope) {
            API_SCOPE -> compilation.legacyApiConfigurationName
            IMPLEMENTATION_SCOPE -> compilation.legacyImplementationConfigurationName
            COMPILE_ONLY_SCOPE -> compilation.legacyCompileOnlyConfigurationName
            RUNTIME_ONLY_SCOPE -> compilation.legacyRuntimeOnlyConfigurationName
        }
    )