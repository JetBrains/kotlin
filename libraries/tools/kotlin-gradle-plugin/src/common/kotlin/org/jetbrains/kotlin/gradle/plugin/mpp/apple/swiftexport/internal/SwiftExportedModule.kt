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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedModule.ExportMode
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import org.jetbrains.kotlin.gradle.utils.getAllDependencies
import java.io.File
import java.io.Serializable

/**
 * Represents a module that will be exported to Swift.
 *
 * @property moduleName The name of the module in Swift
 * @property flattenPackage Optional package flattening configuration
 * @property artifact The artifact file containing the module
 * @property exportMode mode in which this module will be exported
 */
internal interface SwiftExportedModule : Serializable {
    val moduleName: String
    val flattenPackage: String?
    val artifact: File
    val exportMode: ExportMode

    // The reason we're introducing our own enum is that we can't reference classes from swift-export-standalone in this file, as it's
    // not on our runtime classpath.
    enum class ExportMode {
        Full,
        Transitive,
        Excluded,
    }
}

internal fun createFullyExportedSwiftExportedModule(
    moduleName: String,
    flattenPackage: String?,
    artifact: File,
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName = moduleName,
        flattenPackage = flattenPackage,
        artifact = artifact,
        exportMode = ExportMode.Full,
    )
}

internal fun createTransitiveSwiftExportedModule(
    moduleName: String,
    artifact: File,
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName = moduleName,
        flattenPackage = null,
        artifact = artifact,
        exportMode = ExportMode.Transitive,
    )
}

internal fun createExcludedSwiftExportedModule(
    moduleName: String,
    artifact: File,
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName = moduleName,
        flattenPackage = null,
        artifact = artifact,
        exportMode = ExportMode.Excluded,
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
    val artifact: ResolvedArtifactResult,
    val dependency: ResolvedDependencyResult,
) : Serializable {

    // Gradle doesn't guarantee that ResolvedArtifactResult implementations have stable equals/hashCode,
    // so we derive a stable equality key from plain values (Strings) instead of relying on it directly.
    private val equalityKey: String =
        "${moduleVersion.group}:${moduleVersion.name}:${moduleVersion.version}:${artifact.file.absolutePath}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResolvedArtifactWithVersionIdentifier

        return equalityKey == other.equalityKey
    }

    override fun hashCode(): Int {
        return equalityKey.hashCode()
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
            ResolvedArtifactWithVersionIdentifier(moduleVersion, artifacts.single(), dependency)
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
    val missingModules = mutableListOf<SwiftExportedDependency>()

    val artifactsByComponentId = allResolvedArtifacts.associateBy { it.dependency.selected.id }

    // Process all explicitly exported modules. They are fully exported and take precedence over hiddenModules.
    val fullyExportedComponents = mutableSetOf<ResolvedArtifactWithVersionIdentifier>()
    for (explicitModule in exportedModules) {
        val matchingArtifact = allResolvedArtifacts.findMatchingArtifactFor(explicitModule)

        if (matchingArtifact != null) {
            result.add(
                createFullyExportedSwiftExportedModule(
                    moduleName = explicitModule.moduleName.orElse(
                        normalizedAndValidatedModuleName(explicitModule.inheritedName)
                    ).get(),
                    flattenPackage = explicitModule.flattenPackage.orNull,
                    artifact = matchingArtifact.artifact.file
                )
            )

            fullyExportedComponents.add(matchingArtifact)
        } else {
            missingModules.add(explicitModule)
        }
    }

    // Hidden modules, as well as all their transitive dependencies, need to be excluded from export.
    // exportedModules take precedence over hiddenModules, so explicitly exported components are never hidden.
    val hiddenComponents = mutableSetOf<ResolvedArtifactWithVersionIdentifier>()
    for (hiddenModule in hiddenModules) {
        val matchingArtifact = allResolvedArtifacts.findMatchingArtifactFor(hiddenModule)

        if (matchingArtifact != null) {
            hiddenComponents.add(matchingArtifact)
            getAllDependencies(matchingArtifact.dependency).forEach { transitiveDependency ->
                artifactsByComponentId[transitiveDependency.selected.id]?.let { hiddenComponents.add(it) }
            }
        } else {
            missingModules.add(hiddenModule)
        }
    }
    hiddenComponents.removeAll(fullyExportedComponents)
    for (hiddenComponent in hiddenComponents) {
        result.add(
            createExcludedSwiftExportedModule(
                hiddenComponent.moduleVersion.inheritedName.normalizedSwiftExportModuleName,
                hiddenComponent.artifact.file
            )
        )
    }

    if (missingModules.isNotEmpty()) {
        reportDiagnostic(
            KotlinToolingDiagnostics.SwiftExportModuleResolutionError(
                missingModules.map { it.name })
        )
    }

    // resolvedDirectApiArtifacts are treated the same as exportedModules, but don't take precedence over hiddenModules.
    for (apiArtifact in resolvedDirectApiArtifacts) {
        if (apiArtifact in fullyExportedComponents || apiArtifact in hiddenComponents) continue

        result.add(
            createFullyExportedSwiftExportedModule(
                moduleName = apiArtifact.moduleVersion.inheritedName.normalizedSwiftExportModuleName,
                flattenPackage = null,
                artifact = apiArtifact.artifact.file
            )
        )
        fullyExportedComponents.add(apiArtifact)
    }

    // All the remaining components are transitively exported
    allResolvedArtifacts
        .filterNot { artifact -> artifact in fullyExportedComponents }
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
    override val exportMode: ExportMode,
) : SwiftExportedModule

private fun Project.normalizedAndValidatedModuleName(moduleName: String) =
    moduleName.normalizedSwiftExportModuleName.also { validateSwiftExportModuleName(it) }
