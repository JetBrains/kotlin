/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
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
    exportedModulesProvider: Provider<Set<SwiftExportedDependency>>,
): Provider<List<SwiftExportedModule>> = swiftExportConfigurationProvider.zip(exportedModulesProvider) { configuration, modules ->
    configuration.swiftExportedModules(modules, project)
}

internal fun Project.collectApiModules(
    apiConfigurationProvider: Provider<LazyResolvedConfigurationWithArtifacts>,
    exportedModulesProvider: Provider<Set<SwiftExportedDependency>>,
): Provider<String> = apiConfigurationProvider.map { it.root.renderDependencyGraph(
    hiddenDependencies = setOf(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core",
        "project :not-good-looking-project-name",
    )
) }

internal fun Project.collectImplModules(
    implConfigurationProvider: Provider<LazyResolvedConfigurationWithArtifacts>,
    exportedModulesProvider: Provider<Set<SwiftExportedDependency>>,
): Provider<String> = implConfigurationProvider.map { it.root.renderDependencyGraph(
    hiddenDependencies = setOf(
        "com.squareup.okio:okio"
    )
) }

fun ResolvedComponentResult.renderDependencyGraph(
    hiddenDependencies: Set<String> = emptySet()
): String {
    val visited = linkedSetOf<ResolvedComponentResult>()
    val builder = StringBuilder()

    builder.appendLine(displayName())

    val dependencies = dependencies.sortedForStableOutput()
    dependencies.forEachIndexed { index, dependency ->
        renderDependency(
            dependency = dependency,
            prefix = "",
            isLast = index == dependencies.lastIndex,
            depth = 1,
            hiddenDependencies = hiddenDependencies,
            parentIsHidden = false,
            visited = visited,
            builder = builder
        )
    }

    return builder.toString()
}

private fun renderDependency(
    dependency: DependencyResult,
    prefix: String,
    isLast: Boolean,
    depth: Int,
    hiddenDependencies: Set<String>,
    parentIsHidden: Boolean,
    visited: MutableSet<ResolvedComponentResult>,
    builder: StringBuilder
) {
    val branch = if (isLast) "\\--- " else "+--- "
    val childPrefix = prefix + if (isLast) "     " else "|    "
    val directness = if (depth == 1) "direct" else "transitive"

    when (dependency) {
        is ResolvedDependencyResult -> {
            val selected = dependency.selected
            val requested = dependency.requested.displayName
            val selectedName = selected.displayName()
            val selectedHiddenName = selected.hiddenDependencyName()

            val isHidden = selectedHiddenName in hiddenDependencies
            val hiddenStatus = when {
                isHidden -> "hidden"
                parentIsHidden -> "transitively hidden"
                else -> null
            }

            builder.append(prefix)
                .append(branch)
                .append(requested)

            if (requested != selectedName) {
                builder.append(" -> ").append(selectedName)
            }

            builder.append(" (").append(directness)

            if (hiddenStatus != null) {
                builder.append(", ").append(hiddenStatus)
            }

            builder.append(")")

            if (!visited.add(selected)) {
                builder.append(" (*)")
                builder.appendLine()
                return
            }

            builder.appendLine()

            val children = selected.dependencies.sortedForStableOutput()
            children.forEachIndexed { index, child ->
                renderDependency(
                    dependency = child,
                    prefix = childPrefix,
                    isLast = index == children.lastIndex,
                    depth = depth + 1,
                    hiddenDependencies = hiddenDependencies,
                    parentIsHidden = parentIsHidden || isHidden,
                    visited = visited,
                    builder = builder
                )
            }
        }

        is UnresolvedDependencyResult -> {
            val requested = dependency.requested.displayName
            val isHidden = requested in hiddenDependencies
            val hiddenStatus = when {
                isHidden -> "hidden"
                parentIsHidden -> "transitively hidden"
                else -> null
            }

            builder.append(prefix)
                .append(branch)
                .append(requested)
                .append(" FAILED")
                .append(" (")
                .append(directness)

            if (hiddenStatus != null) {
                builder.append(", ").append(hiddenStatus)
            }

            builder.append(")")

            dependency.failure.message?.let { message ->
                builder.append(" - ").append(message.lineSequence().firstOrNull().orEmpty())
            }

            builder.appendLine()
        }

        else -> {
            builder.append(prefix)
                .append(branch)
                .append(dependency.requested.displayName)
                .append(" (")
                .append(directness)

            if (parentIsHidden) {
                builder.append(", transitively hidden")
            }

            builder.appendLine(")")
        }
    }
}

private fun Collection<DependencyResult>.sortedForStableOutput(): List<DependencyResult> =
    sortedWith(
        compareBy<DependencyResult> { it.requested.displayName }
            .thenBy {
                when (it) {
                    is ResolvedDependencyResult -> it.selected.id.displayName
                    else -> ""
                }
            }
    )

private fun ResolvedComponentResult.displayName(): String =
    when (val id = id) {
        is ModuleComponentIdentifier -> "${id.group}:${id.module}:${id.version}"
        is ProjectComponentIdentifier -> "project '${id.projectPath}'"
        else -> id.displayName
    }

private fun ResolvedComponentResult.hiddenDependencyName(): String =
    when (val id = id) {
        is ModuleComponentIdentifier -> "${id.group}:${id.module}"
        is ProjectComponentIdentifier -> "project ${id.projectPath}"
        else -> id.displayName
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

private fun LazyResolvedConfigurationWithArtifacts.swiftExportedModules(
    exportedModules: Set<SwiftExportedDependency>,
    project: Project,
) = project.findAndCreateSwiftExportedModules(exportedModules, filteredArtifacts())

private fun LazyResolvedConfigurationWithArtifacts.filteredArtifacts(): Set<ResolvedArtifactWithVersionIdentifier> {
    return allResolvedDependencies.mapNotNullTo(mutableSetOf()) { dependency ->
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
    resolvedArtifacts: Set<ResolvedArtifactWithVersionIdentifier>,
): List<SwiftExportedModule> {
    val result = mutableListOf<SwiftExportedModule>()
    val processedComponents = mutableSetOf<ResolvedArtifactWithVersionIdentifier>()
    val missingModules = mutableListOf<SwiftExportedDependency>()

    // Process all explicitly exported modules
    for (explicitModule in exportedModules) {
        val matchingArtifact = resolvedArtifacts.find { artifact ->
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

    // Then process remaining components as transitive
    resolvedArtifacts
        .filterNot { artifact -> artifact in processedComponents }
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

private data class SwiftExportedModuleImp(
    override val moduleName: String,
    override val flattenPackage: String?,
    override val artifact: File,
    override val shouldBeFullyExported: Boolean,
) : SwiftExportedModule

private fun Project.normalizedAndValidatedModuleName(moduleName: String) =
    moduleName.normalizedSwiftExportModuleName.also { validateSwiftExportModuleName(it) }
