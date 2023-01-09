/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.ResolvedDependencyGraph
import org.jetbrains.kotlin.gradle.utils.allResolvedDependencies
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

private fun Project.collectAllProjectsData(): Map<String, GranularMetadataTransformation.ProjectData> {
    return rootProject.allprojects.associateBy { it.path }.mapValues { (path, subProject) ->
        GranularMetadataTransformation.ProjectData(
            path = path,
            sourceSetMetadataOutputs = provider { subProject.collectSourceSetMetadataOutputs() },
            projectStructureMetadata = provider { subProject.multiplatformExtensionOrNull?.kotlinProjectStructureMetadata },
            moduleId = provider { ModuleIds.idOfRootModule(subProject) }
        )
    }
}

internal class GranularMetadataTransformation(
    private val params: Params,
    /** A configuration that holds the dependencies of the appropriate scope for all Kotlin source sets in the project */
    private val parentTransformations: Lazy<Iterable<GranularMetadataTransformation>>
) {
    data class Params(
        val sourceSetName: String,
        val resolvedMetadataConfiguration: ResolvedDependencyGraph,
        val sourceSetVisibilityProvider: SourceSetVisibilityProvider,
        val projectStructureMetadataExtractorFactory: MppDependencyProjectStructureMetadataExtractorFactory,
        val projectData: Map<String, ProjectData>,
        val platformCompilationSourceSets: Set<String>,
    ) {
        constructor(project: Project, kotlinSourceSet: KotlinSourceSet): this(
            sourceSetName = kotlinSourceSet.name,
            resolvedMetadataConfiguration = ResolvedDependencyGraph(kotlinSourceSet.internal.resolvableMetadataConfiguration),
            sourceSetVisibilityProvider = SourceSetVisibilityProvider(project),
            projectStructureMetadataExtractorFactory = MppDependencyProjectStructureMetadataExtractorFactory.getOrCreate(project),
            projectData = project.collectAllProjectsData(),
            platformCompilationSourceSets = project.multiplatformExtension.platformCompilationSourceSets
        )
    }

    data class ProjectData(
        val path: String,
        val sourceSetMetadataOutputs: Provider<Map<String, SourceSetMetadataOutputs>>,
        val projectStructureMetadata: Provider<KotlinProjectStructureMetadata?>,
        val moduleId: Provider<ModuleDependencyIdentifier>
    )

    val metadataDependencyResolutions: Iterable<MetadataDependencyResolution> by lazy { doTransform() }

    private fun ComponentIdentifier.toModuleDependencyIdentifier(): ModuleDependencyIdentifier {
        return when(this) {
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(group, module)
            is ProjectComponentIdentifier -> params.projectData[projectPath]?.moduleId?.get()
                ?: error("Cant find project Module ID by $projectPath")
            else -> error("Unknown ComponentIdentifier: $this")
        }
    }

    private fun doTransform(): Iterable<MetadataDependencyResolution> {
        val result = mutableListOf<MetadataDependencyResolution>()

        val parentResolutions =
            parentTransformations.value.flatMap { it.metadataDependencyResolutions }.groupBy {
                it.dependency.id
            }

        val resolvedDependencyQueue: Queue<ResolvedDependencyResult> = ArrayDeque<ResolvedDependencyResult>().apply {
            addAll(
                params.resolvedMetadataConfiguration
                    .root
                    .dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
            )
        }

        val visitedDependencies = mutableSetOf<ComponentIdentifier>()

        while (resolvedDependencyQueue.isNotEmpty()) {
            val resolvedDependency: ResolvedDependencyResult = resolvedDependencyQueue.poll()
            val component = resolvedDependency.selected
            val componentId = component.id

            if (!visitedDependencies.add(componentId)) {
                /* Already processed this dependency */
                continue
            }

            val dependencyResult = processDependency(
                resolvedDependency,
                parentResolutions[componentId].orEmpty()
            )

            result.add(dependencyResult)

            val transitiveDependenciesToVisit = when (dependencyResult) {
                is MetadataDependencyResolution.KeepOriginalDependency ->
                    component.dependencies.filterIsInstance<ResolvedDependencyResult>()

                is MetadataDependencyResolution.ChooseVisibleSourceSets -> dependencyResult.visibleTransitiveDependencies
                is MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency -> dependencyResult.visibleTransitiveDependencies
                is MetadataDependencyResolution.Exclude.Unrequested -> error("a visited dependency is erroneously considered unrequested")
            }

            resolvedDependencyQueue.addAll(
                transitiveDependenciesToVisit.filter { it.selected.id !in visitedDependencies }
            )
        }

        params.resolvedMetadataConfiguration.allResolvedDependencies.forEach { resolvedDependency ->
            if (resolvedDependency.selected.id !in visitedDependencies) {
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
        parentResolutionsForModule: Iterable<MetadataDependencyResolution>,
    ): MetadataDependencyResolution {
        val module = dependency.selected
        val moduleId = module.id

        val compositeMetadataArtifact = params
            .resolvedMetadataConfiguration
            .dependencyArtifacts(dependency)
            .singleOrNull()
            // expected only ony Composite Metadata Klib, but if dependency got resolved into platform variant
            // when source set is a leaf then we might get multiple artifacts in such case we must return KeepOriginal
            ?: return MetadataDependencyResolution.KeepOriginalDependency(module)

        val mppDependencyMetadataExtractor = params.projectStructureMetadataExtractorFactory.create(compositeMetadataArtifact)

        val isResolvedToProject: Boolean = moduleId is ProjectComponentIdentifier && moduleId.build.isCurrentBuild

        val projectStructureMetadata = mppDependencyMetadataExtractor.getProjectStructureMetadata()
            ?: return MetadataDependencyResolution.KeepOriginalDependency(module)

        if (!projectStructureMetadata.isPublishedAsRoot) {
            error("Artifacts of dependency ${moduleId.displayName} is built by old Kotlin Gradle Plugin and can't be consumed in this way")
        }

        val sourceSetVisibility =
            params.sourceSetVisibilityProvider.getVisibleSourceSets(
                params.sourceSetName,
                dependency,
                projectStructureMetadata,
                isResolvedToProject
            )

        val allVisibleSourceSets = sourceSetVisibility.visibleSourceSetNames

        val sourceSetsVisibleInParents = parentResolutionsForModule
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .flatMapTo(mutableSetOf()) { it.allVisibleSourceSetNames }

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
            .filterTo(mutableSetOf()) { it.selected.id.toModuleDependencyIdentifier() in requestedTransitiveDependencies }

        if (params.sourceSetName in params.platformCompilationSourceSets && !isResolvedToProject)
            return MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency(module, transitiveDependenciesToVisit)

        val visibleSourceSetsExcludingDependsOn = allVisibleSourceSets.filterTo(mutableSetOf()) { it !in sourceSetsVisibleInParents }

        val metadataProvider = when (mppDependencyMetadataExtractor) {
            is ProjectMppDependencyProjectStructureMetadataExtractor -> ProjectMetadataProvider(
                sourceSetMetadataOutputs = params.projectData[mppDependencyMetadataExtractor.projectPath]?.sourceSetMetadataOutputs?.get()
                    ?: error("Unexpected project path '${mppDependencyMetadataExtractor.projectPath}'")
            )

            is JarMppDependencyProjectStructureMetadataExtractor -> ArtifactMetadataProvider(
                CompositeMetadataArtifactImpl(
                    moduleDependencyIdentifier = dependency.selected.id.toModuleDependencyIdentifier(),
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

internal val ResolvedComponentResult.projectIdOrNull get(): ProjectComponentIdentifier? {
    val identifier = id
    return when {
        identifier is ProjectComponentIdentifier && identifier.build.isCurrentBuild -> identifier
        else -> null
    }
}

internal fun MetadataDependencyResolution.projectDependency(currentProject: Project): Project? =
    dependency.toProjectOrNull(currentProject)

internal fun ResolvedComponentResult.toProjectOrNull(currentProject: Project): Project? {
    val projectId = projectIdOrNull ?: return null
    return currentProject.project(projectId.projectPath)
}

private val KotlinMultiplatformExtension.platformCompilationSourceSets: Set<String>
    get() = targets.filterNot { it is KotlinMetadataTarget }
        .flatMap { target -> target.compilations }
        .flatMap { it.kotlinSourceSets }
        .map { it.name }
        .toSet()

internal val GranularMetadataTransformation?.metadataDependencyResolutionsOrEmpty get() = this?.metadataDependencyResolutions ?: emptyList()