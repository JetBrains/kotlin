/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportedModuleVersionMetadata
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfiguration
import java.io.File
import java.io.Serializable

internal interface SwiftExportedModule : Serializable {
    val moduleName: String
    val flattenPackage: String?
    val artifact: File
    val shouldBeFullyExported: Boolean
}

internal fun createFullyExportedSwiftExportedModule(
    moduleName: String,
    flattenPackage: String?,
    artifact: File
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName,
        flattenPackage,
        artifact,
        true
    )
}

internal fun Project.collectModules(
    swiftExportConfigurationProvider: Provider<LazyResolvedConfiguration>,
    compileConfigurationProvider: Provider<LazyResolvedConfiguration>,
    exportedModulesProvider: Provider<Set<SwiftExportedModuleVersionMetadata>>,
): Provider<List<SwiftExportedModule>> {
    val swiftExportedModules = swiftExportConfigurationProvider.zip(exportedModulesProvider) { configuration, modules ->
        configuration.swiftExportedModules(modules, project)
    }

    val transitiveModules = compileConfigurationProvider.map { configuration ->
        configuration.transitiveModules(project)
    }

    return swiftExportedModules.zip(transitiveModules) { export, transitive ->
        export.toSet().plus(transitive).toList()
    }
}

private fun LazyResolvedConfiguration.transitiveModules(
    project: Project,
): List<SwiftExportedModule> {
    return artifactComponents().map { (component, artifact) ->
        val resolvedModule = requireNotNull(component.moduleVersion)
        SwiftExportedModuleImp(
            resolvedModule.name.normalizedSwiftExportModuleName.also { project.validateSwiftExportModuleName(it) },
            null,
            artifact,
            false
        )
    }
}

private fun LazyResolvedConfiguration.swiftExportedModules(
    exportedModules: Set<SwiftExportedModuleVersionMetadata>,
    project: Project,
): List<SwiftExportedModule> {
    return artifactComponents().map { (component, artifact) ->
        project.findAndCreateSwiftExportedModule(exportedModules, component, artifact)
    }
}

private fun LazyResolvedConfiguration.artifactComponents(): List<Pair<ResolvedComponentResult, File>> {
    return filteredDependencies().map { component ->
        val dependencyArtifacts = getArtifacts(component)
            .map { it.file }
            .filterNotCinteropKlibs()

        check(dependencyArtifacts.isNotEmpty() && dependencyArtifacts.size == 1) {
            "Component $component ${
                if (dependencyArtifacts.isEmpty())
                    "doesn't have suitable artifacts"
                else
                    "has too many artifacts: $dependencyArtifacts"
            }"
        }

        Pair(component, dependencyArtifacts.single())
    }.distinctBy { (_, artifact) ->
        artifact
    }.toList()
}

private fun LazyResolvedConfiguration.filteredDependencies(): Sequence<ResolvedComponentResult> {
    return allResolvedDependencies.asSequence().filterNot { dependencyResult ->
        dependencyResult.resolvedVariant.owner.let { id -> id is ModuleComponentIdentifier && id.module == "kotlin-stdlib" }
    }.map { it.selected }
}

private val File.isCinteropKlib get() = name.contains("-cinterop-") || name.contains("Cinterop-")
private fun Collection<File>.filterNotCinteropKlibs(): List<File> = filterNot(File::isCinteropKlib)

private fun Project.findAndCreateSwiftExportedModule(
    exportedModules: Set<SwiftExportedModuleVersionMetadata>,
    resolvedComponent: ResolvedComponentResult,
    artifact: File,
): SwiftExportedModule {
    val resolvedModule = requireNotNull(resolvedComponent.moduleVersion)

    val module = exportedModules.single {
        resolvedModule.name == it.moduleVersion.name && resolvedModule.group == it.moduleVersion.group
    }

    return SwiftExportedModuleImp(
        module.moduleName.orElse(
            module.moduleVersion.name.normalizedSwiftExportModuleName.also { validateSwiftExportModuleName(it) }
        ).get(),
        module.flattenPackage.orNull,
        artifact,
        true
    )
}

private data class SwiftExportedModuleImp(
    override val moduleName: String,
    override val flattenPackage: String?,
    override val artifact: File,
    override val shouldBeFullyExported: Boolean,
) : SwiftExportedModule {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SwiftExportedModuleImp

        return artifact == other.artifact
    }

    override fun hashCode(): Int {
        return artifact.hashCode()
    }
}