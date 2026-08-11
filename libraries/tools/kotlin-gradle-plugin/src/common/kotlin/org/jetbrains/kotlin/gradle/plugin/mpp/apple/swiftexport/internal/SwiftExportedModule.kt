/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import java.io.File
import java.io.Serializable

/**
 * Represents a module that will be exported to Swift.
 *
 * @property moduleName The name of the module in Swift
 * @property flattenPackage Optional package flattening configuration
 * @property artifact The artifact file containing the module
 * @property shouldBeFullyExported Whether this module was explicitly requested for export through the swiftExport { export("foo:bar") } DSL
 */
internal interface SwiftExportedModule : Serializable {
    val moduleName: String
    val flattenPackage: String?
    val artifact: File
    val shouldBeFullyExported: Boolean
}

internal fun createFullyExportedSwiftExportedModule(
    moduleName: String,
    flattenPackage: String?,
    artifact: File,
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName,
        flattenPackage,
        artifact,
        true
    )
}

internal fun createTransitiveSwiftExportedModule(
    moduleName: String,
    artifact: File,
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName,
        null,
        artifact,
        false
    )
}

internal fun Project.collectModules(
    swiftExportConfigurationProvider: Provider<LazyResolvedConfigurationWithArtifacts>,
    apiConfigurationProvider: Provider<LazyResolvedConfigurationWithArtifacts>,
    exportedModulesProvider: Provider<Set<SwiftExportedDependency>>,
    hiddenModulesProvider: Provider<Set<SwiftExportedDependency>>,
): Provider<List<SwiftExportedModule>> = swiftExportConfigurationProvider
    .zip(apiConfigurationProvider, ::Pair)
    .zip(exportedModulesProvider.zip(hiddenModulesProvider, ::Pair)) { (exportConfig, apiConfig), (exportedModules, hiddenModules) ->
        swiftExportedModules(exportConfig, apiConfig, exportedModules, hiddenModules)
    }

private class ResolvedArtifactWithVersionIdentifier(
    val moduleVersion: ModuleVersionIdentifier,
    val artifact: ResolvedArtifactResult
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResolvedArtifactWithVersionIdentifier

        return artifact == other.artifact
    }

    override fun hashCode(): Int {
        return 31 * artifact.hashCode()
    }
}

private fun Project.swiftExportedModules(
    swiftExportConfiguration: LazyResolvedConfigurationWithArtifacts,
    apiConfiguration: LazyResolvedConfigurationWithArtifacts,
    exportedModules: Set<SwiftExportedDependency>,
    hiddenModules: Set<SwiftExportedDependency>,
) = findAndCreateSwiftExportedModules(
    exportedModules = exportedModules,
    hiddenModules = hiddenModules,
    allResolvedArtifacts = swiftExportConfiguration.filteredArtifacts(LazyResolvedConfigurationWithArtifacts::allResolvedDependencies),
    resolvedDirectApiArtifacts = apiConfiguration.filteredArtifacts { root.dependencies.filterIsInstance<ResolvedDependencyResult>() },
)

private fun LazyResolvedConfigurationWithArtifacts.filteredArtifacts(
    dependenciesSelector: LazyResolvedConfigurationWithArtifacts.() -> Iterable<ResolvedDependencyResult>
): Set<ResolvedArtifactWithVersionIdentifier> {
    return dependenciesSelector().mapNotNullTo(mutableSetOf()) { dependency ->
        val artifacts = getArtifacts(dependency.selected).filterNot {
            it.file.isCinteropKlib || it.file.isJavaJar
        }

        val moduleVersion = dependency.selected.moduleVersion

        if (artifacts.isNotEmpty() && moduleVersion != null) {
            ResolvedArtifactWithVersionIdentifier(moduleVersion, artifacts.single())
        } else {
            null
        }
    }
}

private val File.isCinteropKlib get() = name.contains("-cinterop-") || name.contains("Cinterop-")
private val File.isJavaJar get() = extension == "jar"

private fun Project.findAndCreateSwiftExportedModules(
    exportedModules: Set<SwiftExportedDependency>,
    hiddenModules: Set<SwiftExportedDependency>,
    allResolvedArtifacts: Set<ResolvedArtifactWithVersionIdentifier>,
    resolvedDirectApiArtifacts: Set<ResolvedArtifactWithVersionIdentifier>,
): List<SwiftExportedModule> {
    val result = mutableListOf<SwiftExportedModule>()
    val processedComponents = mutableSetOf<ResolvedArtifactWithVersionIdentifier>()
    val hiddenComponents = mutableSetOf<ResolvedArtifactWithVersionIdentifier>()
    val missingModules = mutableListOf<SwiftExportedDependency>()

    // Process all explicitly exported modules
    for (explicitModule in exportedModules) {
        val matchingArtifact = allResolvedArtifacts.findMatchingArtifactFor(explicitModule)

        if (matchingArtifact != null) {
            result.add(
                createFullyExportedSwiftExportedModule(
                    explicitModule.moduleName.orElse(
                        normalizedAndValidatedModuleName(explicitModule.inheritedName)
                    ).get(),
                    explicitModule.flattenPackage.orNull,
                    matchingArtifact.artifact.file
                )
            )

            // Track which components we've processed
            processedComponents.add(matchingArtifact)
        } else {
            missingModules.add(explicitModule)
        }
    }

    for (hiddenModule in hiddenModules) {
        val matchingArtifact = allResolvedArtifacts.findMatchingArtifactFor(hiddenModule)

        if (matchingArtifact != null) {
            hiddenComponents.add(matchingArtifact)
        } else {
            missingModules.add(hiddenModule)
        }
    }

    if (missingModules.isNotEmpty()) {
        reportDiagnostic(
            KotlinToolingDiagnostics.SwiftExportModuleResolutionError(
                missingModules.map { it.name })
        )
    }

    // Then process remaining components as transitive
    allResolvedArtifacts
        .filterNot { artifact -> artifact in processedComponents }
        .filterNot { artifact -> artifact in hiddenComponents }
        .forEach { artifact ->
            result.add(
                createTransitiveSwiftExportedModule(
                    artifact.moduleVersion.inheritedName.normalizedSwiftExportModuleName,
                    artifact.artifact.file
                )
            )
        }

    return result
}

private fun Set<ResolvedArtifactWithVersionIdentifier>.findMatchingArtifactFor(
    module: SwiftExportedDependency,
): ResolvedArtifactWithVersionIdentifier? = find { artifact ->
    when (module) {
        is SwiftExportedDependency.External -> {
            // It's a regular external dependency. Match by group and name.
            artifact.moduleVersion.group == module.coordinates.group &&
                    artifact.moduleVersion.name == module.coordinates.name
        }
        is SwiftExportedDependency.Project -> {
            val componentId = artifact.artifact.id.componentIdentifier
            // For project dependencies, we match by project path.
            if (componentId is ProjectComponentIdentifier) {
                // Check if the artifact's project path matches the path stored in our module's name.
                componentId.projectPath == module.projectPath
            } else {
                // This artifact is not from a project, so it cannot be a match.
                false
            }
        }
    }
}

private data class SwiftExportedModuleImp(
    override val moduleName: String,
    override val flattenPackage: String?,
    override val artifact: File,
    override val shouldBeFullyExported: Boolean,
) : SwiftExportedModule

private fun Project.normalizedAndValidatedModuleName(moduleName: String) =
    moduleName.normalizedSwiftExportModuleName.also { validateSwiftExportModuleName(it) }
