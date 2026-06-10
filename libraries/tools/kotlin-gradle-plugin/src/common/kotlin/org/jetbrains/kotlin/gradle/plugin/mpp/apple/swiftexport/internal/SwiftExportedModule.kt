/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import org.jetbrains.kotlin.gradle.utils.loadSingleKlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.isCommonizedCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName
import java.io.File
import java.io.Serializable

internal sealed interface SwiftExportedModule : Serializable {
    /** The klib artifact backing this module. */
    val artifact: File

    /**
     * A Kotlin module translated by Swift Export.
     *
     * @property moduleName The name of the module in Swift
     * @property flattenPackage Optional package flattening configuration
     * @property shouldBeFullyExported Whether this module was explicitly requested for export through the
     * swiftExport { export("foo:bar") } DSL
     */
    sealed interface KotlinModule : SwiftExportedModule {
        val moduleName: String
        val flattenPackage: String?
        val shouldBeFullyExported: Boolean
    }

    data class FullyExported(
        override val moduleName: String,
        override val flattenPackage: String?,
        override val artifact: File,
    ) : KotlinModule {
        override val shouldBeFullyExported: Boolean get() = true
    }

    data class Transitive(
        override val moduleName: String,
        override val artifact: File,
    ) : KotlinModule {
        override val flattenPackage: String? get() = null
        override val shouldBeFullyExported: Boolean get() = false
    }

    /**
     * A cinterop klib whose types Swift Export treats as belonging to the pre-existing Objective-C
     * module [objCModuleName]. No Swift wrapper is generated for it; the consumer is responsible for
     * making the Objective-C module visible to the Swift compiler and linker.
     */
    data class CinteropReexported(
        val objCModuleName: String,
        override val artifact: File,
    ) : SwiftExportedModule
}

internal fun Project.collectModules(
    swiftExportConfigurationProvider: Provider<LazyResolvedConfigurationWithArtifacts>,
    exportedModulesProvider: Provider<Set<SwiftExportedDependency>>,
): Provider<List<SwiftExportedModule>> = swiftExportConfigurationProvider.zip(exportedModulesProvider) { configuration, modules ->
    configuration.swiftExportedModules(modules, project)
}

/**
 * A cinterop klib resolved as an additional artifact of a component.
 *
 * @property cinteropName the name of the `cinterops { ... }` entry that produced the klib, recovered
 * from the klib `unique_name` — KGP composes it as `<prefix>-cinterop-<name>`
 * (see `CreateCInteropTasksSideEffect.baseKlibName`). Used to match `reexportCinterop(...)` declarations.
 */
private class ResolvedCinteropArtifact(
    val cinteropName: String?,
    val file: File,
)

/**
 * Classifies [klib] by its manifest: cinterop klibs always carry `interop = true` in it, while
 * file-name matching may misclassify regular libraries. Returns null for non-cinterop klibs.
 *
 * The manifest can only be read once the klib exists on disk, which is always the case for resolved
 * binary dependencies, but not necessarily for project dependencies — their artifacts may be
 * classified before the producing task has run (e.g. when the configuration cache is stored). Such
 * artifacts are produced by this build, so KGP's own naming conventions apply, and the file name is
 * a reliable fallback.
 */
private fun Project.resolveCinteropArtifact(klib: File): ResolvedCinteropArtifact? {
    if (klib.exists()) {
        val library = loadSingleKlib(klib, reportProblemsAtInfoLevel = true) ?: return null
        if (!library.isCInteropLibrary() && !library.isCommonizedCInteropLibrary()) return null
        return ResolvedCinteropArtifact(library.uniqueName.cinteropNameOrNull(), klib)
    }
    return when {
        // The unpacked klib directory and the maven-style classified artifact are both named after
        // `CInteropProcess.baseKlibName`: `<prefix>-cinterop-<name>`.
        klib.name.contains(CINTEROP_INFIX) -> ResolvedCinteropArtifact(klib.nameWithoutExtension.cinteropNameOrNull(), klib)
        // The packed klib artifact is named after the packing Zip task (see maybeCreateKlibPackingTask):
        // `<project>-<target>Cinterop-<name><compilation>`. The cinterop name cannot be recovered from
        // it unambiguously, so only the classification is derived; the name resolves once the klib is built.
        klib.name.contains(PACKED_CINTEROP_INFIX) -> ResolvedCinteropArtifact(null, klib)
        else -> null
    }
}

private fun String.cinteropNameOrNull(): String? = substringAfterLast(CINTEROP_INFIX, "").ifEmpty { null }

private const val CINTEROP_INFIX = "-cinterop-"
private const val PACKED_CINTEROP_INFIX = "Cinterop-"

/**
 * The resolved klib artifacts of a single component: the main klib plus the cinterop klibs that are
 * published alongside it (KGP publishes each cinterop as an additional artifact of the same variant,
 * classified `cinterop-<name>`; see `CreateCInteropTasksSideEffect`).
 *
 * Equality is keyed on the main artifact so this type can drive a `Set` for deduplication.
 */
private class ResolvedComponentArtifacts(
    val moduleVersion: ModuleVersionIdentifier,
    val mainArtifact: ResolvedArtifactResult,
    val cinteropArtifacts: List<ResolvedCinteropArtifact>,
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResolvedComponentArtifacts

        return mainArtifact == other.mainArtifact
    }

    override fun hashCode(): Int {
        return 31 * mainArtifact.hashCode()
    }
}

private fun LazyResolvedConfigurationWithArtifacts.swiftExportedModules(
    exportedModules: Set<SwiftExportedDependency>,
    project: Project,
) = project.findAndCreateSwiftExportedModules(exportedModules, resolvedComponents(project))

private fun LazyResolvedConfigurationWithArtifacts.resolvedComponents(project: Project): Set<ResolvedComponentArtifacts> {
    return allResolvedDependencies.mapNotNullTo(mutableSetOf()) { dependency ->
        val moduleVersion = dependency.selected.moduleVersion ?: return@mapNotNullTo null
        val klibArtifacts = getArtifacts(dependency.selected).filterNot { it.file.isJavaJar }
        val cinteropByArtifact = klibArtifacts.associateWith { project.resolveCinteropArtifact(it.file) }
        val mainArtifact = klibArtifacts.singleOrNull { cinteropByArtifact[it] == null } ?: return@mapNotNullTo null

        ResolvedComponentArtifacts(
            moduleVersion = moduleVersion,
            mainArtifact = mainArtifact,
            cinteropArtifacts = cinteropByArtifact.values.filterNotNull(),
        )
    }
}

private val File.isJavaJar get() = extension == "jar"

private fun Project.findAndCreateSwiftExportedModules(
    exportedModules: Set<SwiftExportedDependency>,
    resolvedComponents: Set<ResolvedComponentArtifacts>,
): List<SwiftExportedModule> {
    val result = mutableListOf<SwiftExportedModule>()
    val processedComponents = mutableSetOf<ResolvedComponentArtifacts>()
    val missingModules = mutableListOf<SwiftExportedDependency>()

    // Process all explicitly exported modules — matched against the main artifact only.
    for (explicitModule in exportedModules) {
        val matchingComponent = resolvedComponents.find { component ->
            val componentId = component.mainArtifact.id.componentIdentifier

            when (explicitModule) {
                is SwiftExportedDependency.External -> {
                    // It's a regular external dependency. Match by group and name.
                    component.moduleVersion.group == explicitModule.coordinates.group &&
                            component.moduleVersion.name == explicitModule.coordinates.name
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

        if (matchingComponent != null) {
            result.add(
                SwiftExportedModule.FullyExported(
                    explicitModule.moduleName.orElse(
                        normalizedAndValidatedModuleName(explicitModule.inheritedName)
                    ).get(),
                    explicitModule.flattenPackage.orNull,
                    matchingComponent.mainArtifact.file
                )
            )
            result.addAll(
                reexportedCinteropModules(explicitModule, matchingComponent)
            )

            // Track which components we've processed
            processedComponents.add(matchingComponent)
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

    // Then process remaining components as transitive. Their cinterop klibs have no DSL declaration
    // and are deliberately not re-exported: making types of an undeclared Objective-C module part of
    // the exported API surface must be an explicit user decision.
    resolvedComponents
        .filterNot { component -> component in processedComponents }
        .forEach { component ->
            result.add(
                SwiftExportedModule.Transitive(
                    component.moduleVersion.inheritedName.normalizedSwiftExportModuleName,
                    component.mainArtifact.file
                )
            )
        }

    return result
}

/**
 * Matches the `reexportCinterop(...)` declarations of [explicitModule] against the cinterop klibs
 * resolved for [component].
 */
private fun reexportedCinteropModules(
    explicitModule: SwiftExportedDependency,
    component: ResolvedComponentArtifacts,
): List<SwiftExportedModule.CinteropReexported> {
    val declarations = explicitModule.reexportedCinterops.get()
    if (declarations.isEmpty()) return emptyList()

    val cinteropsByName = component.cinteropArtifacts
        .mapNotNull { artifact -> artifact.cinteropName?.let { it to artifact } }
        .toMap()

    return declarations.mapNotNull { (cinteropName, objCModuleName) ->
        val artifact = cinteropsByName[cinteropName] ?: return@mapNotNull null
        if (objCModuleName.isEmpty()) return@mapNotNull null
        SwiftExportedModule.CinteropReexported(objCModuleName, artifact.file)
    }
}

private fun Project.normalizedAndValidatedModuleName(moduleName: String) =
    moduleName.normalizedSwiftExportModuleName.also { validateSwiftExportModuleName(it) }
