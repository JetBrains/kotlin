/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.granularMetadata

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.CompositeMetadataJar
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.Companion.asMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_RUNTIME_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.gradle.utils.ResolvedDependencyGraph
import org.jetbrains.kotlin.gradle.utils.`is`
import java.util.*

data class ProjectId(
    val path: String,
    val buildId: String
)

sealed class DependencyId {
    data class ExternalDependency(
        val name: String,
        val group: String?,
        val version: String?,
        val extras: Map<String, String> // anything else that is used for describing External Dependency
    ) : DependencyId()


    data class ProjectDependency(
        val projectId: ProjectId
    ) : DependencyId()
}

fun DependencyId(dependency: Dependency): DependencyId = when(dependency) {
    is ExternalDependency -> DependencyId.ExternalDependency(
        name = dependency.name,
        group = dependency.group,
        version = dependency.version,
        extras = emptyMap()
    )
    is ProjectDependency -> DependencyId.ProjectDependency(
        ProjectId(
            path = dependency.dependencyProject.path,
            buildId = "UNKNOWN!!!"
        )
    )
    else -> error("Unknown dependency: $dependency")
}