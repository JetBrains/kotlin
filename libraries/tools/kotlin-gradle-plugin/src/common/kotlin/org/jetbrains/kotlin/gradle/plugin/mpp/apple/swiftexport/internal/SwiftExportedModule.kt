/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportedModuleVersionMetadata
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfiguration
import org.jetbrains.kotlin.gradle.utils.dashSeparatedToUpperCamelCase
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import java.io.Serializable

internal interface SwiftExportedModule : Serializable {
    val moduleName: String
    val flattenPackage: String?
    val artifact: File
}

internal fun createSwiftExportedModule(
    moduleName: String,
    flattenPackage: String?,
    artifact: File,
): SwiftExportedModule {
    return SwiftExportedModuleImp(
        moduleName,
        flattenPackage,
        artifact
    )
}

internal fun LazyResolvedConfiguration.swiftExportedModules(
    exportedModules: Set<SwiftExportedModuleVersionMetadata>,
): List<SwiftExportedModule> {
    return allResolvedDependencies.asSequence().filterNot { dependencyResult ->
        dependencyResult.resolvedVariant.owner.let { id -> id is ModuleComponentIdentifier && id.module == "kotlin-stdlib" }
    }.map { it.selected }.map { component ->
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
    }.map { (component, artifact) ->
        findAndCreateSwiftExportedModule(exportedModules, component, artifact)
    }.toList()
}

private val File.isCinteropKlib get() = extension == "klib" && nameWithoutExtension.contains("cinterop-interop")

private fun Collection<File>.filterNotCinteropKlibs(): List<File> = filterNot(File::isCinteropKlib)


private fun findAndCreateSwiftExportedModule(
    exportedModules: Set<SwiftExportedModuleVersionMetadata>,
    resolvedComponent: ResolvedComponentResult,
    artifact: File,
): SwiftExportedModule {
    val resolvedModule = requireNotNull(resolvedComponent.moduleVersion)

    val module = exportedModules.single {
        resolvedModule.name == it.moduleVersion.name && resolvedModule.group == it.moduleVersion.group
    }

    return SwiftExportedModuleImp(
        module.moduleName.orElse(dashSeparatedToUpperCamelCase(module.moduleVersion.name)).get(),
        module.flattenPackage.orNull,
        artifact
    )
}

private data class SwiftExportedModuleImp(
    override val moduleName: String,
    override val flattenPackage: String?,
    override val artifact: File,
) : SwiftExportedModule