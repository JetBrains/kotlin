/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependenciesCollector
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File
import javax.inject.Inject

internal open class DefaultKotlinDependencyHandler @Inject constructor(
    val parent: HasKotlinDependencies,
    override val project: Project,
    val npmDependenciesCollector: KotlinNpmDependenciesCollector,
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
        return project.dependencies.add(configurationName, dependencyNotation)
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
    ): NpmDependencyDeprecated =
        NpmDependencyDeprecated(
            objectFactory = project.objects,
            name = name,
            version = version,
        ).also {
            npmDependenciesCollector.add(
                name = name,
                version = version,
                scope = KotlinNpmDependency.Scope.NORMAL,
            )
        }

    override fun npm(
        name: String,
        directory: File,
    ): NpmDependencyDeprecated =
        directoryNpmDependency(
            name = name,
            directory = directory,
            scope = KotlinNpmDependency.Scope.NORMAL,
        )

    @Suppress("OVERRIDE_DEPRECATION")
    override fun npm(
        directory: File,
    ): NpmDependencyDeprecated =
        npm(
            name = moduleName(directory),
            directory = directory,
        )

    override fun npmDev(
        name: String,
        version: String,
    ) {
        npmDependenciesCollector.add(
            name = name,
            version = version,
            scope = KotlinNpmDependency.Scope.DEV,
        )
    }

    override fun npmDev(
        name: Provider<String>,
        version: Provider<String>,
    ) {
        npmDependenciesCollector.add(
            name = name,
            version = version,
            scope = KotlinNpmDependency.Scope.DEV,
        )
    }

    override fun npmDev(
        name: String,
        directory: File,
    ) {
        npmDependenciesCollector.add(
            name = name,
            file = directory,
            scope = KotlinNpmDependency.Scope.DEV,
        )
    }

    override fun npmDev(
        name: String,
        directory: Provider<Directory>,
    ) {
        npmDependenciesCollector.addDirectory(
            name = name,
            directory = directory,
            scope = KotlinNpmDependency.Scope.DEV,
        )
    }

    override fun npmOptional(
        name: String,
        version: String,
    ) {
        npmDependenciesCollector.add(
            name = name,
            version = version,
            scope = KotlinNpmDependency.Scope.OPTIONAL,
        )
    }

    override fun npmOptional(
        name: Provider<String>,
        version: Provider<String>,
    ) {
        npmDependenciesCollector.add(
            name = name,
            version = version,
            scope = KotlinNpmDependency.Scope.OPTIONAL,
        )
    }

    override fun npmOptional(
        name: String,
        directory: File,
    ) {
        npmDependenciesCollector.add(
            name = name,
            file = directory,
            scope = KotlinNpmDependency.Scope.OPTIONAL,
        )
    }

    override fun npmOptional(
        name: String,
        directory: Provider<Directory>,
    ) {
        npmDependenciesCollector.addDirectory(
            name = name,
            directory = directory,
            scope = KotlinNpmDependency.Scope.OPTIONAL,
        )
    }

    override fun npmPeer(
        name: String,
        version: String,
    ) {
        npmDependenciesCollector.add(
            name = name,
            version = version,
            scope = KotlinNpmDependency.Scope.PEER,
        )
    }

    override fun npmPeer(
        name: Provider<String>,
        version: Provider<String>,
    ) {
        npmDependenciesCollector.add(
            name = name,
            version = version,
            scope = KotlinNpmDependency.Scope.PEER,
        )
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun devNpm(
        name: String,
        version: String
    ): NpmDependencyDeprecated =
        NpmDependencyDeprecated(
            objectFactory = project.objects,
            name = name,
            version = version,
            scope = NpmDependencyScopeDeprecated.DEV
        ).also {
            npmDependenciesCollector.add(
                name = name,
                version = version,
                scope = KotlinNpmDependency.Scope.DEV,
            )
        }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun devNpm(
        name: String,
        directory: File
    ): NpmDependencyDeprecated =
        directoryNpmDependency(
            name = name,
            directory = directory,
            scope = KotlinNpmDependency.Scope.DEV,
        )

    @Suppress("OVERRIDE_DEPRECATION")
    override fun devNpm(directory: File): NpmDependencyDeprecated =
        devNpm(
            name = moduleName(directory),
            directory = directory
        )

    @Suppress("OVERRIDE_DEPRECATION")
    override fun optionalNpm(
        name: String,
        version: String,
    ): NpmDependencyDeprecated =
        NpmDependencyDeprecated(
            objectFactory = project.objects,
            name = name,
            version = version,
            scope = NpmDependencyScopeDeprecated.OPTIONAL,
        ).also {
            npmDependenciesCollector.add(
                name = name,
                version = version,
                scope = KotlinNpmDependency.Scope.OPTIONAL,
            )
        }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun optionalNpm(
        name: String,
        directory: File,
    ): NpmDependencyDeprecated =
        directoryNpmDependency(
            name = name,
            directory = directory,
            scope = KotlinNpmDependency.Scope.OPTIONAL,
        )

    @Suppress("OVERRIDE_DEPRECATION")
    override fun optionalNpm(
        directory: File,
    ): NpmDependencyDeprecated =
        optionalNpm(
            name = moduleName(directory),
            directory = directory,
        )

    @Suppress("OVERRIDE_DEPRECATION")
    override fun peerNpm(
        name: String,
        version: String
    ): NpmDependencyDeprecated =
        NpmDependencyDeprecated(
            objectFactory = project.objects,
            name = name,
            version = version,
            scope = NpmDependencyScopeDeprecated.PEER
        ).also {
            npmDependenciesCollector.add(
                name = name,
                version = version,
                scope = KotlinNpmDependency.Scope.PEER,
            )
        }

    private fun directoryNpmDependency(
        name: String,
        directory: File,
        scope: KotlinNpmDependency.Scope,
    ): NpmDependencyDeprecated =
        directoryNpmDependency(
            objectFactory = project.objects,
            name = name,
            directory = directory,
            scope = scope.toDeprecatedScope(),
        ).also {
            npmDependenciesCollector.add(
                name = name,
                file = directory,
                scope = scope,
            )
        }
}

internal fun ObjectFactory.DefaultKotlinDependencyHandler(
    parent: HasKotlinDependencies,
    project: Project,
    npmDependenciesCollector: KotlinNpmDependenciesCollector,
) = newInstance<DefaultKotlinDependencyHandler>(parent, project, npmDependenciesCollector)

private fun KotlinNpmDependency.Scope.toDeprecatedScope(): NpmDependencyScopeDeprecated =
    when (this) {
        KotlinNpmDependency.Scope.NORMAL -> NpmDependencyScopeDeprecated.NORMAL
        KotlinNpmDependency.Scope.DEV -> NpmDependencyScopeDeprecated.DEV
        KotlinNpmDependency.Scope.OPTIONAL -> NpmDependencyScopeDeprecated.OPTIONAL
        KotlinNpmDependency.Scope.PEER -> NpmDependencyScopeDeprecated.PEER
    }
