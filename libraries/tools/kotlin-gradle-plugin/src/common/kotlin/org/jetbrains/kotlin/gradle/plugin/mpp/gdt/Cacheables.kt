/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.gdt

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.currentBuildId

internal sealed class CacheableDependency {
    data class External(
        //@get:Input
        val group: String?,
        //@get:Input
        val name: String,
        //@get:Input
        val version: String?
    ) : CacheableDependency()

    data class Project(
        val project: CacheableProject
    ) : CacheableDependency()
}

internal class CacheableProject(
    val buildId: String,
    val path: String,
    val name: String,
    val group: String,
    val isMpp: Boolean
)

internal fun CacheableProject(project: Project) = with(project) {
    if (multiplatformExtensionOrNull != null) {
        val rootPublication = multiplatformExtension.rootSoftwareComponent.publicationDelegate
        val group = rootPublication?.groupId ?: group.toString()
        val name = rootPublication?.artifactId ?: name
        CacheableProject(
            buildId = currentBuildId().name,
            path = path,
            name = name,
            group = group,
            isMpp = true
        )
    } else {
        CacheableProject(
            buildId = currentBuildId().name,
            path = path,
            name = name,
            group = group.toString(),
            isMpp = false
        )
    }
}

internal fun CacheableDependency(dependency: Dependency) =
    when (dependency) {
        is ExternalModuleDependency -> CacheableDependency.External(
            group = dependency.group,
            name = dependency.name,
            version = dependency.version
        )
        is ProjectDependency -> CacheableDependency.Project(CacheableProject(dependency.dependencyProject))
        else -> error("Unsupported Dependency: $dependency of type ${dependency.javaClass}")
    }

internal sealed class CacheableMetadataDependencyResolution(
    val dependency: ResolvedComponentResult,
    val projectPath: String?
) {
    override fun toString(): String {
        val verb = when (this) {
            is KeepOriginalDependency -> "keep"
            is ExcludeAsUnrequested -> "exclude"
            is ChooseVisibleSourceSets -> "choose"
        }
        return "$verb, dependency = $dependency"
    }

    class KeepOriginalDependency(
        dependency: ResolvedComponentResult,
        projectPath: String?
    ) : CacheableMetadataDependencyResolution(dependency, projectPath)

    class ExcludeAsUnrequested(
        dependency: ResolvedComponentResult,
        projectPath: String?
    ) : CacheableMetadataDependencyResolution(dependency, projectPath)

    class ChooseVisibleSourceSets internal constructor(
        dependency: ResolvedComponentResult,
        projectPath: String?,
        val projectStructureMetadata: KotlinProjectStructureMetadata,
        val allVisibleSourceSetNames: Set<String>,
        val visibleSourceSetNamesExcludingDependsOn: Set<String>,
        val visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
        //internal val metadataProvider: ChooseVisibleSourceSets.MetadataProvider
    ) : CacheableMetadataDependencyResolution(dependency, projectPath) {
        override fun toString(): String =
            super.toString() + ", sourceSets = " + allVisibleSourceSetNames.joinToString(", ", "[", "]") {
                (if (it in visibleSourceSetNamesExcludingDependsOn) "*" else "") + it
            }
    }
}