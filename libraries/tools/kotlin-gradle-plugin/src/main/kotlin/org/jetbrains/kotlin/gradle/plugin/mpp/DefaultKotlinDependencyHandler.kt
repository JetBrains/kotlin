/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.directoryNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.moduleName
import org.jetbrains.kotlin.gradle.targets.js.npm.onlyNameNpmDependency
import java.io.File

class DefaultKotlinDependencyHandler(
    val parent: HasKotlinDependencies,
    val project: Project
) : KotlinDependencyHandler {
    override fun api(dependencyNotation: Any): Dependency? =
        addDependencyByAnyNotation(parent.apiConfigurationName, dependencyNotation)

    override fun api(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency =
        addDependencyByStringNotation(parent.apiConfigurationName, dependencyNotation, configure)

    override fun <T : Dependency> api(dependency: T, configure: T.() -> Unit): T =
        addDependency(parent.apiConfigurationName, dependency, configure)

    override fun implementation(dependencyNotation: Any): Dependency? =
        addDependencyByAnyNotation(parent.implementationConfigurationName, dependencyNotation)

    override fun implementation(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency =
        addDependencyByStringNotation(parent.implementationConfigurationName, dependencyNotation, configure)

    override fun <T : Dependency> implementation(dependency: T, configure: T.() -> Unit): T =
        addDependency(parent.implementationConfigurationName, dependency, configure)

    override fun compileOnly(dependencyNotation: Any): Dependency? =
        addDependencyByAnyNotation(parent.compileOnlyConfigurationName, dependencyNotation)

    override fun compileOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency =
        addDependencyByStringNotation(parent.compileOnlyConfigurationName, dependencyNotation, configure)

    override fun <T : Dependency> compileOnly(dependency: T, configure: T.() -> Unit): T =
        addDependency(parent.compileOnlyConfigurationName, dependency, configure)

    override fun runtimeOnly(dependencyNotation: Any): Dependency? =
        addDependencyByAnyNotation(parent.runtimeOnlyConfigurationName, dependencyNotation)

    override fun runtimeOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency =
        addDependencyByStringNotation(parent.runtimeOnlyConfigurationName, dependencyNotation, configure)

    override fun <T : Dependency> runtimeOnly(dependency: T, configure: T.() -> Unit): T =
        addDependency(parent.runtimeOnlyConfigurationName, dependency, configure)

    override fun kotlin(simpleModuleName: String, version: String?): ExternalModuleDependency =
        project.dependencies.create(
            "org.jetbrains.kotlin:kotlin-$simpleModuleName" + version?.let { ":$it" }.orEmpty()
        ) as ExternalModuleDependency

    override fun project(notation: Map<String, Any?>): ProjectDependency =
        project.dependencies.project(notation) as ProjectDependency

    private fun addDependencyByAnyNotation(
        configurationName: String,
        dependencyNotation: Any
    ): Dependency? =
        project.dependencies.add(configurationName, dependencyNotation)

    private fun addDependencyByStringNotation(
        configurationName: String,
        dependencyNotation: Any,
        configure: ExternalModuleDependency.() -> Unit = { }
    ): ExternalModuleDependency =
        addDependency(configurationName, project.dependencies.create(dependencyNotation) as ExternalModuleDependency, configure)

    private fun <T : Dependency> addDependency(
        configurationName: String,
        dependency: T,
        configure: T.() -> Unit
    ): T =
        dependency.also {
            configure(it)
            project.dependencies.add(configurationName, it)
        }

    override fun npm(name: String): Dependency =
        onlyNameNpmDependency(name)

    override fun npm(
        name: String,
        version: String,
        generateExternals: Boolean
    ): NpmDependency =
        NpmDependency(
            project = project,
            name = name,
            version = version,
            generateExternals = generateExternals
        )

    override fun npm(
        name: String,
        version: String
    ): NpmDependency =
        npm(
            name = name,
            version = version,
            generateExternals = defaultGenerateExternals()
        )

    override fun npm(
        name: String,
        directory: File,
        generateExternals: Boolean
    ): NpmDependency =
        directoryNpmDependency(
            name = name,
            directory = directory,
            scope = NpmDependency.Scope.NORMAL,
            generateExternals = generateExternals
        )

    override fun npm(
        name: String,
        directory: File
    ): NpmDependency =
        npm(
            name = name,
            directory = directory,
            generateExternals = defaultGenerateExternals()
        )

    override fun npm(
        directory: File,
        generateExternals: Boolean
    ): NpmDependency =
        npm(
            name = moduleName(directory),
            directory = directory,
            generateExternals = generateExternals
        )

    override fun npm(directory: File): NpmDependency =
        npm(
            directory = directory,
            generateExternals = defaultGenerateExternals()
        )

    override fun devNpm(
        name: String,
        version: String
    ): NpmDependency =
        NpmDependency(
            project = project,
            name = name,
            version = version,
            scope = NpmDependency.Scope.DEV
        )

    override fun devNpm(
        name: String,
        directory: File
    ): NpmDependency =
        directoryNpmDependency(
            name = name,
            directory = directory,
            scope = NpmDependency.Scope.DEV,
            generateExternals = false
        )

    override fun devNpm(directory: File): NpmDependency =
        devNpm(
            name = moduleName(directory),
            directory = directory
        )

    override fun optionalNpm(
        name: String,
        version: String,
        generateExternals: Boolean
    ): NpmDependency =
        NpmDependency(
            project = project,
            name = name,
            version = version,
            scope = NpmDependency.Scope.OPTIONAL,
            generateExternals = generateExternals
        )

    override fun optionalNpm(
        name: String,
        version: String
    ): NpmDependency =
        optionalNpm(
            name,
            version,
            defaultGenerateExternals()
        )

    override fun optionalNpm(
        name: String,
        directory: File,
        generateExternals: Boolean
    ): NpmDependency =
        directoryNpmDependency(
            name = name,
            directory = directory,
            scope = NpmDependency.Scope.OPTIONAL,
            generateExternals = generateExternals
        )

    override fun optionalNpm(name: String, directory: File): NpmDependency =
        optionalNpm(
            name = name,
            directory = directory,
            generateExternals = defaultGenerateExternals()
        )

    override fun optionalNpm(
        directory: File,
        generateExternals: Boolean
    ): NpmDependency =
        optionalNpm(
            name = moduleName(directory),
            directory = directory,
            generateExternals = generateExternals
        )

    override fun optionalNpm(directory: File): NpmDependency =
        optionalNpm(
            directory = directory,
            generateExternals = defaultGenerateExternals()
        )

    override fun peerNpm(
        name: String,
        version: String
    ): NpmDependency =
        NpmDependency(
            project = project,
            name = name,
            version = version,
            scope = NpmDependency.Scope.PEER
        )

    private fun defaultGenerateExternals(): Boolean =
        PropertiesProvider(project).jsGenerateExternals

    private fun directoryNpmDependency(
        name: String,
        directory: File,
        scope: NpmDependency.Scope,
        generateExternals: Boolean
    ): NpmDependency =
        directoryNpmDependency(
            project = project,
            name = name,
            directory = directory,
            scope = scope,
            generateExternals = generateExternals
        )
}
