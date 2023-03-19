/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.directoryNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.moduleName
import java.io.File

class DefaultKotlinDependencyHandler(
    val parent: HasKotlinDependencies,
    override val project: Project
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
    ): Dependency? {
        val dependency = when (dependencyNotation) {
            is GradleKpmModule -> project.dependencies.create(dependencyNotation.project).apply {
                (this as ModuleDependency).capabilities {
                    if (dependencyNotation.moduleClassifier != null) {
                        it.requireCapability(ComputedCapability.fromModule(dependencyNotation))
                    }
                }
            }
            else -> dependencyNotation
        }
        return project.dependencies.add(configurationName, dependency)
    }

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

    override fun npm(
        name: String,
        version: String,
    ): NpmDependency =
        NpmDependency(
            objectFactory = project.objects,
            name = name,
            version = version,
        )

    override fun npm(
        name: String,
        directory: File,
    ): NpmDependency =
        directoryNpmDependency(
            name = name,
            directory = directory,
            scope = NpmDependency.Scope.NORMAL,
        )

    override fun npm(
        directory: File,
    ): NpmDependency =
        npm(
            name = moduleName(directory),
            directory = directory,
        )

    override fun devNpm(
        name: String,
        version: String
    ): NpmDependency =
        NpmDependency(
            objectFactory = project.objects,
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
        )

    override fun devNpm(directory: File): NpmDependency =
        devNpm(
            name = moduleName(directory),
            directory = directory
        )

    override fun optionalNpm(
        name: String,
        version: String,
    ): NpmDependency =
        NpmDependency(
            objectFactory = project.objects,
            name = name,
            version = version,
            scope = NpmDependency.Scope.OPTIONAL,
        )

    override fun optionalNpm(
        name: String,
        directory: File,
    ): NpmDependency =
        directoryNpmDependency(
            name = name,
            directory = directory,
            scope = NpmDependency.Scope.OPTIONAL,
        )

    override fun optionalNpm(
        directory: File,
    ): NpmDependency =
        optionalNpm(
            name = moduleName(directory),
            directory = directory,
        )

    override fun peerNpm(
        name: String,
        version: String
    ): NpmDependency =
        NpmDependency(
            objectFactory = project.objects,
            name = name,
            version = version,
            scope = NpmDependency.Scope.PEER
        )

    private fun directoryNpmDependency(
        name: String,
        directory: File,
        scope: NpmDependency.Scope,
    ): NpmDependency =
        directoryNpmDependency(
            objectFactory = project.objects,
            name = name,
            directory = directory,
            scope = scope,
        )
}
