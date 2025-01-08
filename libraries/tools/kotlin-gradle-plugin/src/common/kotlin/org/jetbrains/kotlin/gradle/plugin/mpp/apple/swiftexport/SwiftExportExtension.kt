/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.exportedSwiftExportApiConfigurationName
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import javax.inject.Inject

interface SwiftExportedModuleMetadata {
    /**
     * Configure name of the swift export module from this project.
     */
    @get:Input
    @get:Optional
    val moduleName: Property<String>

    /**
     * Configure package collapsing rule.
     */
    @get:Input
    @get:Optional
    val flattenPackage: Property<String>
}

interface SwiftExportAdvancedConfiguration {
    /**
     * Configure SwiftExportConfig.settings parameters
     */
    @get:Input
    @get:Optional
    val settings: MapProperty<String, String>
}

interface SwiftExportedModuleVersionMetadata : SwiftExportedModuleMetadata {
    /**
     * Module version identifier
     */
    @get:Internal
    val moduleVersion: ModuleVersionIdentifier
}

internal fun ObjectFactory.SwiftExportExtension(dependencies: DependencyHandler): SwiftExportExtension =
    newInstance(SwiftExportExtension::class.java, dependencies)

@ExperimentalSwiftExportDsl
@Suppress("unused", "MemberVisibilityCanBePrivate") // Public API
abstract class SwiftExportExtension @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
    private val dependencyHandler: DependencyHandler,
) : SwiftExportedModuleMetadata {

    /**
     * Configure Link task.
     */
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
    fun linkTask(configure: Action<KotlinNativeLink>) = linkTask {
        configure.execute(this)
    }

    /**
     * Configure Swift Export Advanced parameters.
     */
    fun configure(configure: SwiftExportAdvancedConfiguration.() -> Unit = {}) {
        advancedConfiguration.configure()
    }

    /**
     * Configure Swift Export Advanced parameters.
     */
    fun configure(configure: Action<SwiftExportAdvancedConfiguration>) = configure {
        configure.execute(this)
    }

    /**
     * Configure Swift Export modules export.
     */
    fun export(dependency: Any, configure: SwiftExportedModuleMetadata.() -> Unit = {}) {
        val dependencyProvider: Provider<Dependency> = when (dependency) {
            is Provider<*> -> dependency.map { dep ->
                when (dep) {
                    is Dependency -> dep
                    else -> dependencyHandler.create(dep)
                }
            }
            else -> providerFactory.provider { dependencyHandler.create(dependency) }
        }

        forAllSwiftExportBinaries {
            val swiftExportCompilation = target.compilations.getByName(SwiftExportConstants.SWIFT_EXPORT_COMPILATION)
            val compileDependencyConfiguration = swiftExportCompilation.internal.configurations.compileDependencyConfiguration
            val exportedSwiftExportApiConfigurationName = target.exportedSwiftExportApiConfigurationName(buildType)

            dependencyHandler.addProvider(exportedSwiftExportApiConfigurationName, dependencyProvider)
            dependencyHandler.addProvider(
                compileDependencyConfiguration.name,
                dependencyProvider
            )

            project.configurations.getByName(exportedSwiftExportApiConfigurationName).shouldResolveConsistentlyWith(
                compileDependencyConfiguration
            )
        }

        val dependencyId = dependencyProvider.map { dep ->
            val moduleGroup = dep.group
            val moduleName = dep.name
            val moduleVersion = dep.version

            getCoordinatesFromGroupNameAndVersion(moduleGroup, moduleName, moduleVersion)
        }

        addToExportedModules(
            objectFactory.ModuleExport(dependencyId, configure)
        )
    }

    /**
     * Configure Swift Export modules export.
     */
    fun export(dependency: Any, configure: Action<SwiftExportedModuleMetadata>) = export(dependency) {
        configure.execute(this)
    }

    /**
     * Returns a list of exported modules.
     */
    internal val exportedModules: Provider<Set<SwiftExportedModuleVersionMetadata>> = providerFactory.provider {
        _exportedModules
    }

    /**
     * Advanced configuration settings.
     */
    internal val advancedConfiguration = objectFactory.newInstance(SwiftExportAdvancedConfiguration::class.java)

    private val _swiftExportBinaries = objectFactory.domainObjectSet<AbstractNativeLibrary>()

    internal fun addBinary(binary: AbstractNativeLibrary) {
        _swiftExportBinaries.add(binary)
    }

    private val _exportedModules = objectFactory.namedDomainObjectSet<ModuleExport>()

    private fun addToExportedModules(module: ModuleExport) {
        check(_exportedModules.findByName(module.name) == null) {
            "Project already has Export module with name ${module.name}"
        }

        _exportedModules.add(module)
    }

    private fun forAllSwiftExportBinaries(configure: AbstractNativeLibrary.() -> Unit) {
        _swiftExportBinaries.configureEach(configure)
    }
}

private abstract class ModuleExport @Inject constructor(
    moduleVersionProvider: Provider<ModuleVersionIdentifier>,
) : SwiftExportedModuleVersionMetadata, Named {
    @get:Internal
    override val moduleVersion: ModuleVersionIdentifier by moduleVersionProvider

    @Internal
    override fun getName(): String = moduleVersion.let {
        "${it.group}:${it.name}:${it.version}"
    }
}

private fun ObjectFactory.ModuleExport(
    dependencyId: Provider<ModuleVersionIdentifier>,
    configure: SwiftExportedModuleMetadata.() -> Unit = {},
): ModuleExport {
    return newInstance(ModuleExport::class.java, dependencyId).also {
        it.configure()
    }
}