/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SWIFT_EXPORT_METADATA_SCHEMA_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDeclaredModuleOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDependencySelector
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.applySwiftExportConsumerOverrides
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.deserializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import java.io.File
import java.io.Serializable

/**
 * KGP-side copy of `SwiftModuleExportMode`. The standalone type can't be used here: `swift-export-standalone` is
 * `compileOnly` in KGP and this value is serialized into worker parameters. [SwiftExportAction] maps between the two.
 */
internal enum class SwiftExportedModuleMode {
    /** The whole public API is translated. */
    FULL,

    /** Only what fully exported modules refer to is translated. */
    TRANSITIVE,

    /**
     * Only the types other modules refer to are emitted, as empty stubs. The klib stays on the analysis path,
     * so declarations in other modules that mention its types are still exported.
     */
    HIDDEN,
}

/**
 * Represents a module that will be exported to Swift.
 *
 * @property moduleName The name of the module in Swift
 * @property rootPackage Optional root package configuration, used for package flattening only with [SwiftExportedModuleMode.FULL]
 * @property artifact The artifact file containing the module
 * @property exportMode How Swift Export translates this module
 */
internal interface SwiftExportedModule : Serializable {
    val moduleName: String
    val rootPackage: String?
    val artifact: File
    val exportMode: SwiftExportedModuleMode
}

internal fun createFullyExportedSwiftExportedModule(
    moduleName: String,
    rootPackage: String?,
    artifact: File,
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName,
        rootPackage,
        artifact,
        SwiftExportedModuleMode.FULL
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
        SwiftExportedModuleMode.TRANSITIVE
    )
}

internal fun createHiddenSwiftExportedModule(
    moduleName: String,
    artifact: File,
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName,
        null,
        artifact,
        SwiftExportedModuleMode.HIDDEN
    )
}

internal fun Project.collectModules(
    exportConfigurationProvider: Provider<LazyResolvedConfigurationWithArtifacts>,
    apiConfigurationProvider: Provider<LazyResolvedConfigurationWithArtifacts?>,
    metadataConfigurationProvider: Provider<LazyResolvedConfigurationWithArtifacts?>,
    exportedModulesProvider: Provider<Set<SwiftExportedDependency>>,
    dependencyOptionsOverridesProvider: Provider<Map<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>>,
    rootModuleNameProvider: Provider<String>,
): Provider<List<SwiftExportedModule>> = provider {
    val exportConfiguration = exportConfigurationProvider.get()
    val apiConfiguration = apiConfigurationProvider.orNull
    val metadataConfiguration = metadataConfigurationProvider.orNull
    val exportedModules = exportedModulesProvider.get()
    val dependencyOptionsOverrides = dependencyOptionsOverridesProvider.get()
    val rootModuleName = rootModuleNameProvider.get()

    project.applySwiftExportConsumerOverrides(
        modules = project.swiftExportedModules(
            exportConfiguration = exportConfiguration,
            apiConfiguration = apiConfiguration,
            exportedModules = exportedModules,
        ),
        overrides = dependencyOptionsOverrides,
        metadataByComponent = metadataConfiguration?.metadataByComponent(project::reportDiagnostic) ?: emptyMap(),
        exportConfiguration = exportConfiguration,
        apiConfiguration = apiConfiguration,
        rootModuleName = rootModuleName,
    )
}

private class ResolvedArtifactWithVersionIdentifier(
    val moduleVersion: ModuleVersionIdentifier,
    val artifact: ResolvedArtifactResult,
) : Serializable {
    private val artifactFilePath: String get() = artifact.file.absolutePath

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResolvedArtifactWithVersionIdentifier

        return artifactFilePath == other.artifactFilePath
    }

    override fun hashCode(): Int {
        return 31 * artifactFilePath.hashCode()
    }

    fun defaultExportedModuleName(): String {
        val componentId = artifact.id.componentIdentifier
        return defaultSwiftExportModuleName(componentId, moduleVersion)
            ?: error("Unexpected component identifier type: ${componentId::class}")
    }
}

/**
 * Default name of a fully exported module, before normalization: the project path for a project, the coordinates
 * for an external module, `null` for anything else. Also used by the consumer overrides, so a promoted dependency
 * is named like an `api` one.
 */
internal fun defaultSwiftExportModuleName(id: ComponentIdentifier, moduleVersion: ModuleVersionIdentifier?): String? =
    when (id) {
        is ProjectComponentIdentifier -> id.projectPath
        is ModuleComponentIdentifier -> moduleVersion?.inheritedName
        else -> null
    }

private fun Project.swiftExportedModules(
    exportConfiguration: LazyResolvedConfigurationWithArtifacts,
    apiConfiguration: LazyResolvedConfigurationWithArtifacts?,
    exportedModules: Set<SwiftExportedDependency>,
) = findAndCreateSwiftExportedModules(
    exportedModules = exportedModules,
    resolvedExportArtifacts = exportConfiguration.filteredArtifacts { allResolvedDependencies },
    resolvedDirectApiArtifacts = apiConfiguration
        ?.filteredArtifacts {
            root.dependencies
                .filterIsInstance<ResolvedDependencyResult>()
                .filterNot { it.isConstraint }
        }
        ?: emptySet(),
)

/**
 * Reads the Swift Export metadata published by each resolved dependency, keyed by the owning component so it can be
 * correlated with the klib artifacts collected from the same dependency graph.
 *
 * The receiver is expected to be resolved requesting the `swiftExportMetadata` variant, so [resolvedArtifacts] contains
 * only the metadata JSONs. Dependencies without such a variant are simply absent (lenient artifact view). Artifacts that
 * are missing on disk are silently skipped. Metadata carrying an unsupported [SwiftExportMetadata.schemaVersion] is
 * skipped with a warning ([KotlinToolingDiagnostics.SwiftExportUnsupportedMetadataSchemaVersion]), as well as metadata that
 * fails to decode - ([KotlinToolingDiagnostics.SwiftExportMalformedMetadata]).
 */
internal fun LazyResolvedConfigurationWithArtifacts.metadataByComponent(
    reportDiagnostic: (ToolingDiagnostic) -> Unit,
): Map<ComponentIdentifier, SwiftExportMetadata> {
    return resolvedArtifacts.mapNotNull { artifact ->
        if (!artifact.file.exists()) return@mapNotNull null
        val metadata = try {
            artifact.file.inputStream().use(::deserializeSwiftExportMetadata)
        } catch (e: Exception) {
            reportDiagnostic(
                KotlinToolingDiagnostics.SwiftExportMalformedMetadata(
                    artifact.id.componentIdentifier.displayName,
                    e.message,
                )
            )
            return@mapNotNull null
        }
        if (metadata.schemaVersion != SWIFT_EXPORT_METADATA_SCHEMA_VERSION) {
            reportDiagnostic(
                KotlinToolingDiagnostics.SwiftExportUnsupportedMetadataSchemaVersion(
                    artifact.id.componentIdentifier.displayName,
                    metadata.schemaVersion,
                    SWIFT_EXPORT_METADATA_SCHEMA_VERSION,
                )
            )
            return@mapNotNull null
        }
        artifact.id.componentIdentifier to metadata
    }.toMap()
}

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
    resolvedExportArtifacts: Set<ResolvedArtifactWithVersionIdentifier>,
    resolvedDirectApiArtifacts: Set<ResolvedArtifactWithVersionIdentifier>,
): List<SwiftExportedModule> {
    val result = mutableListOf<SwiftExportedModule>()
    val processedComponents = mutableSetOf<ResolvedArtifactWithVersionIdentifier>()
    val missingModules = mutableListOf<SwiftExportedDependency>()

    // Process all explicitly exported modules
    for (explicitModule in exportedModules) {
        val matchingArtifact = resolvedExportArtifacts.find { artifact ->
            val componentId = artifact.artifact.id.componentIdentifier

            when (explicitModule) {
                is SwiftExportedDependency.External -> {
                    // It's a regular external dependency. Match by group and name.
                    artifact.moduleVersion.group == explicitModule.coordinates.group &&
                            artifact.moduleVersion.name == explicitModule.coordinates.name
                }
                is SwiftExportedDependency.Project -> {
                    // For project dependencies, we match by project path.
                    if (componentId is ProjectComponentIdentifier) {
                        // Check if the artifact's project path matches the path stored in our module's name.
                        componentId.projectPath == explicitModule.projectPath
                    } else {
                        // This artifact is not from a project, so it cannot be a match.
                        false
                    }
                }
            }
        }

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

    if (missingModules.isNotEmpty()) {
        reportDiagnostic(
            KotlinToolingDiagnostics.SwiftExportModuleResolutionError(
                missingModules.map { it.name })
        )
    }

    for (artifact in resolvedDirectApiArtifacts) {
        if (artifact in processedComponents) continue
        result.add(
            createFullyExportedSwiftExportedModule(
                moduleName = artifact.defaultExportedModuleName().normalizedSwiftExportModuleName,
                rootPackage = null,
                artifact = artifact.artifact.file,
            )
        )
        // Track which components we've processed
        processedComponents.add(artifact)
    }

    // Then process remaining components as transitive
    resolvedExportArtifacts
        .filterNot { artifact -> artifact in processedComponents }
        .forEach { artifact ->
            result.add(
                createTransitiveSwiftExportedModule(
                    moduleName = artifact.moduleVersion.inheritedName.normalizedSwiftExportModuleName,
                    artifact = artifact.artifact.file
                )
            )
        }

    return result
}

private data class SwiftExportedModuleImp(
    override val moduleName: String,
    override val rootPackage: String?,
    override val artifact: File,
    override val exportMode: SwiftExportedModuleMode,
) : SwiftExportedModule

private fun Project.normalizedAndValidatedModuleName(moduleName: String) =
    moduleName.normalizedSwiftExportModuleName.also { validateSwiftExportModuleName(it) }
