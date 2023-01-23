/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinDependencyHandler

interface KotlinCompilationConfigurationsContainer {
    val deprecatedCompileConfiguration: Configuration?
    val deprecatedRuntimeConfiguration: Configuration?
    val apiConfiguration: Configuration
    val implementationConfiguration: Configuration
    val compileOnlyConfiguration: Configuration
    val runtimeOnlyConfiguration: Configuration
    val compileDependencyConfiguration: Configuration
    val runtimeDependencyConfiguration: Configuration?
    val hostSpecificMetadataConfiguration: Configuration?
    val pluginConfiguration: Configuration
}

internal class DefaultKotlinCompilationConfigurationsContainer(
    override val deprecatedCompileConfiguration: Configuration?,
    override val deprecatedRuntimeConfiguration: Configuration?,
    override val apiConfiguration: Configuration,
    override val implementationConfiguration: Configuration,
    override val compileOnlyConfiguration: Configuration,
    override val runtimeOnlyConfiguration: Configuration,
    override val compileDependencyConfiguration: Configuration,
    override val runtimeDependencyConfiguration: Configuration?,
    override val hostSpecificMetadataConfiguration: Configuration?,
    override val pluginConfiguration: Configuration
) : KotlinCompilationConfigurationsContainer

internal fun HasKotlinDependencies(
    project: Project, compilationDependencyContainer: KotlinCompilationConfigurationsContainer
): HasKotlinDependencies = object : HasKotlinDependencies {
    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configure: Action<KotlinDependencyHandler>) =
        dependencies { configure.execute(this) }

    override val apiConfigurationName: String
        get() = compilationDependencyContainer.apiConfiguration.name

    override val implementationConfigurationName: String
        get() = compilationDependencyContainer.implementationConfiguration.name

    override val compileOnlyConfigurationName: String
        get() = compilationDependencyContainer.compileOnlyConfiguration.name

    override val runtimeOnlyConfigurationName: String
        get() = compilationDependencyContainer.runtimeOnlyConfiguration.name
}
