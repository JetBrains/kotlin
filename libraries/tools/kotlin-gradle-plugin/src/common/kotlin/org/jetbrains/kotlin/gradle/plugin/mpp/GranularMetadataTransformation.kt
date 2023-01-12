/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.mergeWith
import java.util.*

internal sealed class MetadataDependencyResolution(
    val dependency: ResolvedComponentResult,
) {
    /** Evaluate and store the value, as the [dependency] will be lost during Gradle instant execution */
//    val originalArtifactFiles: List<File> = dependency.dependents.flatMap {  it.allModuleArtifacts } .map { it.file }

    override fun toString(): String {
        val verb = when (this) {
            is KeepOriginalDependency -> "keep"
            is Exclude -> "exclude"
            is ChooseVisibleSourceSets -> "choose"
        }
        return "$verb, dependency = $dependency"
    }

    class KeepOriginalDependency(
        dependency: ResolvedComponentResult
    ) : MetadataDependencyResolution(dependency)

    sealed class Exclude(
        dependency: ResolvedComponentResult
    ) : MetadataDependencyResolution(dependency) {

        class Unrequested(
            dependency: ResolvedComponentResult
        ) : Exclude(dependency)

        /**
         * Resolution for metadata dependencies of leaf platform source sets.
         * They are excluded since platform source sets should receive
         * platform dependencies from corresponding compilations and should not get metadata ones.
         * See KT-52216
         */
        class PublishedPlatformSourceSetDependency(
            dependency: ResolvedComponentResult,
            val visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
        ) : Exclude(dependency)
    }

    class ChooseVisibleSourceSets internal constructor(
        dependency: ResolvedComponentResult,
        val projectStructureMetadata: KotlinProjectStructureMetadata,
        val allVisibleSourceSetNames: Set<String>,
        val visibleSourceSetNamesExcludingDependsOn: Set<String>,
        val visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
        internal val metadataProvider: MetadataProvider
    ) : MetadataDependencyResolution(dependency) {

        internal sealed class MetadataProvider {
            class ArtifactMetadataProvider(private val compositeMetadataArtifact: CompositeMetadataArtifact) :
                MetadataProvider(), CompositeMetadataArtifact by compositeMetadataArtifact

            abstract class ProjectMetadataProvider : MetadataProvider() {
                enum class MetadataConsumer { Ide, Cli }

                abstract fun getSourceSetCompiledMetadata(sourceSetName: String): FileCollection
                abstract fun getSourceSetCInteropMetadata(sourceSetName: String, consumer: MetadataConsumer): FileCollection
            }
        }

        override fun toString(): String =
            super.toString() + ", sourceSets = " + allVisibleSourceSetNames.joinToString(", ", "[", "]") {
                (if (it in visibleSourceSetNamesExcludingDependsOn) "*" else "") + it
            }
    }
}

private typealias ComponentIdentifierKey = String
/**
 * This unique key can be used to lookup various info for related Resolved Dependency
 * that gets serialized
 */
private val ComponentIdentifier.uniqueKey get(): ComponentIdentifierKey =
    when (this) {
        is ProjectComponentIdentifier -> "project ${build.name} :$projectPath"
        is ModuleComponentIdentifier -> "module $group:$module:$version"
        else -> error("Unexpected Component Identifier: '$this' of type $javaClass")
    }

internal class GranularMetadataTransformation(
    val project: Project,
    val kotlinSourceSet: KotlinSourceSet,
    parentVisibleSourceSetsProvider: () -> Iterable<Map<ComponentIdentifierKey, Set<String>>>
) {
    private val parentVisibleSourceSets: Map<ComponentIdentifierKey, Set<String>> by lazy {
        parentVisibleSourceSetsProvider().reduceOrNull { acc, map -> acc mergeWith map }.orEmpty()
    }

    val metadataDependencyResolutions: Iterable<MetadataDependencyResolution> by lazy { doTransform() }

    val visibleSourceSetsByComponentId: Map<ComponentIdentifierKey, Set<String>> = metadataDependencyResolutions
        .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
        .associate { it.dependency.id.uniqueKey to it.allVisibleSourceSetNames }

    private val requestedDependencies: Iterable<Dependency> by lazy {
        kotlinSourceSet.internal.resolvableMetadataConfiguration.incoming.dependencies
    }

    internal val configurationToResolve: Configuration = kotlinSourceSet.internal.resolvableMetadataConfiguration

    private fun doTransform(): Iterable<MetadataDependencyResolution> {
        val result = mutableListOf<MetadataDependencyResolution>()

        val allRequestedDependencies = requestedDependencies

        val resolutionResult = configurationToResolve.incoming.resolutionResult
        val allModuleDependencies =
            configurationToResolve.incoming.resolutionResult.allDependencies.filterIsInstance<ResolvedDependencyResult>()

        val resolvedDependencyQueue: Queue<ResolvedDependencyResult> = ArrayDeque<ResolvedDependencyResult>().apply {
            val requestedModules: Set<ModuleDependencyIdentifier> = allRequestedDependencies.mapTo(mutableSetOf()) {
                ModuleIds.fromDependency(it)
            }

            addAll(
                resolutionResult.root.dependencies
                    .filter { ModuleIds.fromComponentSelector(project, it.requested) in requestedModules }
                    .filterIsInstance<ResolvedDependencyResult>()
            )
        }

        val visitedDependencies = mutableSetOf<ResolvedDependencyResult>()

        while (resolvedDependencyQueue.isNotEmpty()) {
            val resolvedDependency: ResolvedDependencyResult = resolvedDependencyQueue.poll()
            val selectedComponent = resolvedDependency.selected

            if (!visitedDependencies.add(resolvedDependency)) {
                /* Already processed this dependency */
                continue
            }

            val dependencyResult = processDependency(
                resolvedDependency,
                parentVisibleSourceSets[selectedComponent.id.uniqueKey].orEmpty()
            )

            result.add(dependencyResult)

            val transitiveDependenciesToVisit = when (dependencyResult) {
                is MetadataDependencyResolution.KeepOriginalDependency ->
                    selectedComponent.dependencies.filterIsInstance<ResolvedDependencyResult>()

                is MetadataDependencyResolution.ChooseVisibleSourceSets -> dependencyResult.visibleTransitiveDependencies
                is MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency -> dependencyResult.visibleTransitiveDependencies
                is MetadataDependencyResolution.Exclude.Unrequested -> error("a visited dependency is erroneously considered unrequested")
            }

            resolvedDependencyQueue.addAll(
                transitiveDependenciesToVisit.filter { it !in visitedDependencies }
            )
        }

        allModuleDependencies.forEach { resolvedDependency ->
            if (resolvedDependency !in visitedDependencies) {
//                val files = resolvedDependency.moduleArtifacts.map { it.file }
                result.add(
                    MetadataDependencyResolution.Exclude.Unrequested(
                        resolvedDependency.selected,
                    )
                )
            }
        }

        return result
    }

    /**
     * If the [module] is an MPP metadata module, we extract [KotlinProjectStructureMetadata] and do the following:
     *
     * * get the [KotlinProjectStructureMetadata] from the dependency (either deserialize from the artifact or build from the project)
     *
     * * determine the set *S* of source sets that should be seen in the [kotlinSourceSet] by finding which variants the [parent]
     *   dependency got resolved for the compilations where [kotlinSourceSet] participates:
     *
     * * transform the single Kotlin metadata artifact into a set of Kotlin metadata artifacts for the particular source sets in
     *   *S* and add the results as [MetadataDependencyResolution.ChooseVisibleSourceSets]
     *
     * * based on the project structure metadata, determine which of the module's dependencies are requested by the
     *   source sets in *S*, then consider only these transitive dependencies, ignore the others;
     */
    private fun processDependency(
        dependency: ResolvedDependencyResult,
        sourceSetsVisibleInParents: Set<String>,
    ): MetadataDependencyResolution {
        val module = dependency.selected
        val mppDependencyMetadataExtractor = MppDependencyProjectStructureMetadataExtractor.create(
            project, module, configurationToResolve,
            resolveViaAvailableAt = false // we will process the available-at module as a dependency later in the queue
        )

        val resolvedToProject: Project? = module.toProjectOrNull(project)

        val projectStructureMetadata = mppDependencyMetadataExtractor?.getProjectStructureMetadata()
            ?: return MetadataDependencyResolution.KeepOriginalDependency(module)

        if (!projectStructureMetadata.isPublishedAsRoot) {
            error("Artifacts of dependency ${module.id.displayName} is built by old Kotlin Gradle Plugin and can't be consumed in this way")
        }

        val sourceSetVisibility =
            SourceSetVisibilityProvider(project).getVisibleSourceSets(
                kotlinSourceSet.name,
                dependency,
                projectStructureMetadata,
                resolvedToProject != null
            )

        val allVisibleSourceSets = sourceSetVisibility.visibleSourceSetNames

        // Keep only the transitive dependencies requested by the visible source sets:
        // Visit the transitive dependencies visible by parents, too (i.e. allVisibleSourceSets), as this source set might get a more
        // concrete view on them:
        val requestedTransitiveDependencies: Set<ModuleDependencyIdentifier> =
            mutableSetOf<ModuleDependencyIdentifier>().apply {
                projectStructureMetadata.sourceSetModuleDependencies.forEach { (sourceSetName, moduleDependencies) ->
                    if (sourceSetName in allVisibleSourceSets) {
                        addAll(moduleDependencies.map { ModuleDependencyIdentifier(it.groupId, it.moduleId) })
                    }
                }
            }

        val transitiveDependenciesToVisit = module.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .filterTo(mutableSetOf()) { ModuleIds.fromComponent(project, it.selected) in requestedTransitiveDependencies }

        if (kotlinSourceSet in project.multiplatformExtension.platformCompilationSourceSets && resolvedToProject == null)
            return MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency(module, transitiveDependenciesToVisit)

        val visibleSourceSetsExcludingDependsOn = allVisibleSourceSets.filterTo(mutableSetOf()) { it !in sourceSetsVisibleInParents }

        val metadataProvider = when (mppDependencyMetadataExtractor) {
            is ProjectMppDependencyProjectStructureMetadataExtractor -> ProjectMetadataProvider(
                dependencyProject = project.project(mppDependencyMetadataExtractor.projectPath),
                moduleIdentifier = mppDependencyMetadataExtractor.moduleIdentifier
            )

            is JarMppDependencyProjectStructureMetadataExtractor -> ArtifactMetadataProvider(
                CompositeMetadataArtifactImpl(
                    moduleDependencyIdentifier = ModuleIds.fromComponent(project, module),
                    moduleDependencyVersion = module.moduleVersion?.version ?: "unspecified",
                    kotlinProjectStructureMetadata = projectStructureMetadata,
                    primaryArtifactFile = mppDependencyMetadataExtractor.primaryArtifactFile,
                    hostSpecificArtifactFilesBySourceSetName = sourceSetVisibility.hostSpecificMetadataArtifactBySourceSet
                )
            )
        }

        return MetadataDependencyResolution.ChooseVisibleSourceSets(
            dependency = module,
            projectStructureMetadata = projectStructureMetadata,
            allVisibleSourceSetNames = allVisibleSourceSets,
            visibleSourceSetNamesExcludingDependsOn = visibleSourceSetsExcludingDependsOn,
            visibleTransitiveDependencies = transitiveDependenciesToVisit,
            metadataProvider = metadataProvider
        )
    }
}

internal val ResolvedComponentResult.currentBuildProjectIdOrNull get(): ProjectComponentIdentifier? {
    val identifier = id
    return when {
        identifier is ProjectComponentIdentifier && identifier.build.isCurrentBuild -> identifier
        else -> null
    }
}

internal fun MetadataDependencyResolution.projectDependency(currentProject: Project): Project? =
    dependency.toProjectOrNull(currentProject)

internal fun ResolvedComponentResult.toProjectOrNull(currentProject: Project): Project? {
    val projectId = currentBuildProjectIdOrNull ?: return null
    return currentProject.project(projectId.projectPath)
}

private val KotlinMultiplatformExtension.platformCompilationSourceSets: Set<KotlinSourceSet>
    get() = targets.filterNot { it is KotlinMetadataTarget }
        .flatMap { target -> target.compilations }
        .flatMap { it.kotlinSourceSets }
        .toSet()

internal val GranularMetadataTransformation?.metadataDependencyResolutionsOrEmpty get() = this?.metadataDependencyResolutions ?: emptyList()