/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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
        val compileScopes = listOf(KotlinDependencyScope.API_SCOPE, KotlinDependencyScope.IMPLEMENTATION_SCOPE, KotlinDependencyScope.COMPILE_ONLY_SCOPE)
    }
}

internal fun Project.sourceSetDependencyConfigurationByScope(sourceSet: HasKotlinDependencies, scope: KotlinDependencyScope): Configuration =
    project.configurations.getByName(
        when (scope) {
            API_SCOPE -> sourceSet.apiConfigurationName
            IMPLEMENTATION_SCOPE -> sourceSet.implementationConfigurationName
            COMPILE_ONLY_SCOPE -> sourceSet.compileOnlyConfigurationName
            RUNTIME_ONLY_SCOPE -> sourceSet.runtimeOnlyConfigurationName
        }
    )

internal fun Project.compilationDependencyConfigurationByScope(
    compilation: KotlinCompilation<*>,
    scope: KotlinDependencyScope
): Configuration =
    project.configurations.getByName(
        when (scope) {
            API_SCOPE -> compilation.apiConfigurationName
            IMPLEMENTATION_SCOPE -> compilation.implementationConfigurationName
            COMPILE_ONLY_SCOPE -> compilation.compileOnlyConfigurationName
            RUNTIME_ONLY_SCOPE -> compilation.runtimeOnlyConfigurationName
        }
    )

internal fun Project.sourceSetMetadataConfigurationByScope(sourceSet: KotlinSourceSet, scope: KotlinDependencyScope): Configuration =
    project.configurations.getByName(
        when (scope) {
            API_SCOPE -> sourceSet.apiMetadataConfigurationName
            IMPLEMENTATION_SCOPE -> sourceSet.implementationMetadataConfigurationName
            COMPILE_ONLY_SCOPE -> sourceSet.compileOnlyMetadataConfigurationName
            RUNTIME_ONLY_SCOPE -> sourceSet.runtimeOnlyMetadataConfigurationName
        }
    )
