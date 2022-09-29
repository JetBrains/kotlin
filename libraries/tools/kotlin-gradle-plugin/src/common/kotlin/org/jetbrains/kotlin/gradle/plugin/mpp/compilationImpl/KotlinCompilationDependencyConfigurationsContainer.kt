/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.utils.*

internal interface KotlinCompilationDependencyConfigurationsContainer {
    val apiConfiguration: Configuration
    val implementationConfiguration: Configuration
    val compileOnlyConfiguration: Configuration
    val runtimeOnlyConfiguration: Configuration
    val compileDependencyConfiguration: Configuration
    val runtimeDependencyConfiguration: Configuration?
}

internal class DefaultKotlinCompilationDependencyConfigurationsContainer(
    override val apiConfiguration: Configuration,
    override val implementationConfiguration: Configuration,
    override val compileOnlyConfiguration: Configuration,
    override val runtimeOnlyConfiguration: Configuration,
    override val compileDependencyConfiguration: Configuration,
    override val runtimeDependencyConfiguration: Configuration?
) : KotlinCompilationDependencyConfigurationsContainer

internal fun HasKotlinDependencies(
    project: Project, compilationDependencyContainer: KotlinCompilationDependencyConfigurationsContainer
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
