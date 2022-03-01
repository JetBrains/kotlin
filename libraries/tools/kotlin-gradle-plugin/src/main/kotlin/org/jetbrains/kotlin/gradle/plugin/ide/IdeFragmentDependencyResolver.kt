/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalIdeApi::class)

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.getSourceSetCompiledMetadata
import org.jetbrains.kotlin.gradle.kpm.FragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.kpm.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.kpm.KotlinGradleModule.Companion.moduleName
import org.jetbrains.kotlin.gradle.kpm.kpmModules
import org.jetbrains.kotlin.gradle.kpm.toModuleDependency
import org.jetbrains.kotlin.gradle.plugin.sources.SourceSetMetadataStorageForIde
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier
import org.jetbrains.kotlin.project.model.LocalModuleIdentifier

/**
 * Called by the IDE during import to resolve dependencies on a certain fragment
 */
@InternalIdeApi
interface IdeFragmentDependencyResolver {
    val project: Project
    fun resolveDependencies(moduleName: String, fragmentName: String): List<IdeFragmentDependency>

    companion object {
        @JvmStatic
        fun create(project: Project): IdeFragmentDependencyResolver =
            IdeFragmentDependencyResolverImpl(project, FragmentGranularMetadataResolverFactory())
    }
}

internal fun IdeFragmentDependencyResolver.resolveDependencies(kotlinGradleFragment: KotlinGradleFragment): List<IdeFragmentDependency> {
    require(kotlinGradleFragment.project == project) {
        "IdeFragmentDependencyResolver for project ${project.path} can't resolve dependencies for foreign fragment " +
                "${kotlinGradleFragment.project.path}:${kotlinGradleFragment.containingModule.name}/${kotlinGradleFragment.name}"
    }
    return resolveDependencies(kotlinGradleFragment.containingModule.name, kotlinGradleFragment.name)
}

private class IdeFragmentDependencyResolverImpl(
    override val project: Project,
    private val fragmentGranularMetadataResolverFactory: FragmentGranularMetadataResolverFactory
) : IdeFragmentDependencyResolver {
    override fun resolveDependencies(moduleName: String, fragmentName: String): List<IdeFragmentDependency> {
        val fragment = project.kpmModules.getByName(moduleName).fragments.getByName(fragmentName)
        val fragmentGranularMetadataResolver = fragmentGranularMetadataResolverFactory.getOrCreate(fragment)
        return fragmentGranularMetadataResolver.resolutions.flatMap { resolution ->
            resolveDependenciesForIde(fragment, resolution)
        }
    }
}

private fun resolveDependenciesForIde(
    fragment: KotlinGradleFragment, resolution: MetadataDependencyResolution
): List<IdeFragmentDependency> {
    val moduleIdentifier = resolution.dependency.toModuleDependency().moduleIdentifier
    return when (val dependencyId = resolution.dependency.id) {
        is ProjectComponentIdentifier ->
            resolveLocalSourceMetadataDependencies(resolution, dependencyId, moduleIdentifier)
        is ModuleComponentIdentifier ->
            resolveMavenBinaryMetadataDependencies(fragment, resolution, dependencyId, moduleIdentifier)
        else -> emptyList()
    }
}

private fun resolveLocalSourceMetadataDependencies(
    resolution: MetadataDependencyResolution,
    dependencyId: ProjectComponentIdentifier,
    kotlinModuleIdentifier: KotlinModuleIdentifier
): List<IdeLocalSourceFragmentDependency> {
    if (kotlinModuleIdentifier !is LocalModuleIdentifier) return emptyList()
    if (resolution !is MetadataDependencyResolution.ChooseVisibleSourceSets) return emptyList()
    return resolution.allVisibleSourceSetNames.map { fragmentName ->
        IdeLocalSourceFragmentDependency(
            buildId = dependencyId.build,
            projectPath = dependencyId.projectPath,
            projectName = dependencyId.projectName,
            kotlinModuleName = kotlinModuleIdentifier.moduleName,
            kotlinFragmentName = fragmentName
        )
    }
}

private fun resolveMavenBinaryMetadataDependencies(
    fragment: KotlinGradleFragment,
    resolution: MetadataDependencyResolution,
    dependencyId: ModuleComponentIdentifier,
    kotlinModuleIdentifier: KotlinModuleIdentifier,
): List<IdeMavenBinaryFragmentDependency> {
    return when (resolution) {
        is MetadataDependencyResolution.ChooseVisibleSourceSets ->
            resolveMavenBinaryMetadataDependencies(fragment, resolution, dependencyId, kotlinModuleIdentifier)
        is MetadataDependencyResolution.KeepOriginalDependency -> emptyList() // TODO
        is MetadataDependencyResolution.ExcludeAsUnrequested -> emptyList()
    }
}

private fun resolveMavenBinaryMetadataDependencies(
    fragment: KotlinGradleFragment,
    resolution: MetadataDependencyResolution.ChooseVisibleSourceSets,
    dependencyId: ModuleComponentIdentifier,
    kotlinModuleIdentifier: KotlinModuleIdentifier,
): List<IdeMavenBinaryFragmentDependency> {
    return resolution.allVisibleSourceSetNames.map { fragmentName ->
        IdeMavenBinaryFragmentDependency(
            mavenGroup = dependencyId.group,
            mavenModule = dependencyId.module,
            kotlinModuleName = kotlinModuleIdentifier.moduleName,
            kotlinFragmentName = fragmentName,
            version = dependencyId.version,
            files = resolution.metadataProvider.getSourceSetCompiledMetadata(
                project = fragment.project,
                sourceSetName = fragmentName,
                outputDirectoryWhenMaterialised = SourceSetMetadataStorageForIde.sourceSetStorage(fragment.project, fragmentName),
                materializeFilesIfNecessary = true
            ).toList()
        )
    }
}
