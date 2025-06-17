/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.exportedSwiftExportApiConfigurationName
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.moduleVersionIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
import org.jetbrains.kotlin.gradle.utils.namedDomainObjectSet
import javax.inject.Inject

/**
 * Represents metadata for a Swift exported module in a project.
 * This interface provides configuration options for defining the
 * exported module's name and package collapsing rules.
 *
 * This API is experimental and may change in future versions.
 */
@ExperimentalSwiftExportDsl
interface SwiftExportedModuleMetadata {
    /**
     * Configure name of the swift export module from this project.
     */
    val moduleName: Property<String>

    /**
     * Configure package collapsing rule.
     */
    val flattenPackage: Property<String>
}

/**
 * Represents the advanced configuration for exporting Swift code.
 *
 * This API is experimental and may change in future versions.
 */
@ExperimentalSwiftExportDsl
interface SwiftExportAdvancedConfiguration {
    /**
     * Configure SwiftExportConfig.settings parameters
     */
    val settings: MapProperty<String, String>

    /**
     * Specifies additional compiler arguments to be passed to the compiler.
     */
    val freeCompilerArgs: ListProperty<String>
}

internal fun ObjectFactory.SwiftExportExtension(dependencies: DependencyHandler): SwiftExportExtension =
    newInstance(SwiftExportExtension::class.java, dependencies)

/**
 * An *experimental* plugin DSL extension to configure Swift Export.
 *
 * Swift Export is a part of the Kotlin toolset designed to generate Swift code from Kotlin source files.
 * You can use this tool to create Swift bindings for your Kotlin multiplatform libraries.
 *
 * This extension is available inside the `kotlin {}` block in your build script:
 *
 * ```kotlin
 * kotlin {
 *     swiftExport {
 *         // Your Swift Export configuration
 *     }
 * }
 * ```
 *
 * Note that this DSL is experimental, and it will likely change in future versions until it is stable.
 *
 * @since 2.1.0
 */
/*
We can't mark top level extensions with @ExperimentalSwiftExportDsl because
in buildSrc Gradle always creates accessors for these extensions which cause the opt-in error,
which cannot be suppressed.

See Gradle issue https://github.com/gradle/gradle/issues/32019
 */
@KotlinGradlePluginDsl
abstract class SwiftExportExtension @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
    private val dependencyHandler: DependencyHandler,
) : SwiftExportedModuleMetadata {
    /**
     * Configure Link task.
     */
    @ExperimentalSwiftExportDsl
    fun linkTask(configure: KotlinNativeLink.() -> Unit = {}) {
        forAllSwiftExportBinaries {
            linkTaskProvider.configure { linkTask ->
                configure(linkTask)
            }
        }
    }

    /**
     * Configure Link task.
     */
    @ExperimentalSwiftExportDsl
    fun linkTask(configure: Action<KotlinNativeLink>) = linkTask {
        configure.execute(this)
    }

    /**
     * Configure Swift Export Advanced parameters.
     */
    @ExperimentalSwiftExportDsl
    fun configure(configure: SwiftExportAdvancedConfiguration.() -> Unit = {}) {
        advancedConfiguration.configure()
    }

    /**
     * Configure Swift Export Advanced parameters.
     */
    @ExperimentalSwiftExportDsl
    fun configure(configure: Action<SwiftExportAdvancedConfiguration>) = configure {
        configure.execute(this)
    }

    /**
     * Configure Swift Export modules export.
     */
    @ExperimentalSwiftExportDsl
    fun export(dependency: Any, configure: SwiftExportedModuleMetadata.() -> Unit = {}) {
        when (dependency) {
            is Provider<*> -> {
                addDependencyToExportConfiguration(dependency.map { dep ->
                    if (dep is Dependency) dep else dependencyHandler.create(dep)
                })
                addDependencyToExportedModules(dependency, configure)
            }
            else -> {
                addDependencyToExportConfiguration(providerFactory.provider { dependencyHandler.create(dependency) })
                addDependencyToExportedModules(providerFactory.provider { dependency }, configure)
            }
        }
    }

    /**
     * Configure Swift Export modules export.
     */
    @ExperimentalSwiftExportDsl
    fun export(dependency: Any, configure: Action<SwiftExportedModuleMetadata>) = export(dependency) {
        configure.execute(this)
    }

    /**
     * Returns a list of exported modules.
     */
    internal val exportedModules: Provider<Set<SwiftExportedDependency>> = providerFactory.provider {
        _exportedModules
    }

    /**
     * Advanced configuration settings.
     */
    internal val advancedConfiguration: SwiftExportAdvancedConfiguration = DefaultSwiftExportAdvancedConfiguration(objectFactory)

    private val _swiftExportBinaries = objectFactory.domainObjectSet<AbstractNativeLibrary>()

    internal fun addBinary(binary: AbstractNativeLibrary) {
        _swiftExportBinaries.add(binary)
    }

    private val _exportedModules = objectFactory.namedDomainObjectSet<SwiftExportedDependency>()

    private fun addDependencyToExportedModules(dependency: Provider<*>, configure: SwiftExportedModuleMetadata.() -> Unit) {
        val dependencyId = dependency.map { dep ->
            when (dep) {
                is Project -> SwiftExportedDependency.Project(objectFactory, dep.path)
                is ProjectDependency -> SwiftExportedDependency.Project(objectFactory, dep.path)
                is Dependency -> SwiftExportedDependency.External(objectFactory, dep.moduleVersionIdentifier)
                else -> SwiftExportedDependency.External(objectFactory, dependencyHandler.create(dep).moduleVersionIdentifier)
            }.also {
                it.configure()
            }
        }

        _exportedModules.addLater(dependencyId)
    }

    private fun addDependencyToExportConfiguration(dependency: Provider<Dependency>) {
        forAllSwiftExportBinaries {
            val swiftExportCompilation = target.compilations.getByName(SwiftExportConstants.SWIFT_EXPORT_COMPILATION)
            val compileDependencyConfiguration = swiftExportCompilation.internal.configurations.compileDependencyConfiguration
            val exportedSwiftExportApiConfigurationName = target.exportedSwiftExportApiConfigurationName(buildType)

            dependencyHandler.addProvider(exportedSwiftExportApiConfigurationName, dependency)
            dependencyHandler.addProvider(
                compileDependencyConfiguration.name,
                dependency
            )

            project.configurations.getByName(exportedSwiftExportApiConfigurationName).shouldResolveConsistentlyWith(
                compileDependencyConfiguration
            )
        }
    }

    private fun forAllSwiftExportBinaries(configure: AbstractNativeLibrary.() -> Unit) {
        _swiftExportBinaries.configureEach(configure)
    }
}

private class DefaultSwiftExportAdvancedConfiguration(objectFactory: ObjectFactory) : SwiftExportAdvancedConfiguration {
    override val settings: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)
    override val freeCompilerArgs: ListProperty<String> = objectFactory.listProperty(String::class.java)
}