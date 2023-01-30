/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfiguration
import org.jetbrains.kotlin.gradle.utils.getOrPut
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
                abstract fun getSourceSetCInteropMetadata(sourceSetName: String, consumer: MetadataConsumer): FileCollection?
            }
        }

        override fun toString(): String =
            super.toString() + ", sourceSets = " + allVisibleSourceSetNames.joinToString(", ", "[", "]") {
                (if (it in visibleSourceSetNamesExcludingDependsOn) "*" else "") + it
            }
    }
}

internal class GranularMetadataTransformation(
    val params: Params,
    val parentSourceSetVisibilityProvider: ParentSourceSetVisibilityProvider
) {
    private val logger = Logging.getLogger("GranularMetadataTransformation[${params.sourceSetName}]")

    class Params(
        val sourceSetName: String,
        val resolvedMetadataConfiguration: LazyResolvedConfiguration,
        val sourceSetVisibilityProvider: SourceSetVisibilityProvider,
        val projectStructureMetadataExtractorFactory: MppDependencyProjectStructureMetadataExtractorFactory,
        val projectData: Map<String, ProjectData>,
        val platformCompilationSourceSets: Set<String>,
    ) {
        constructor(project: Project, kotlinSourceSet: KotlinSourceSet) : this(
            sourceSetName = kotlinSourceSet.name,
            resolvedMetadataConfiguration = LazyResolvedConfiguration(kotlinSourceSet.internal.resolvableMetadataConfiguration),
            sourceSetVisibilityProvider = SourceSetVisibilityProvider(project),
            projectStructureMetadataExtractorFactory = project.kotlinMppDependencyProjectStructureMetadataExtractorFactory,
            projectData = project.allProjectsData,
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

    val visibleSourceSetsByComponentId: Map<ComponentIdentifier, Set<String>> by lazy {
        metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .groupBy { it.dependency.id }
            .mapValues { (_, visibleSourceSets) -> visibleSourceSets.flatMap { it.allVisibleSourceSetNames }.toSet() }
    }

    private fun doTransform(): Iterable<MetadataDependencyResolution> {
        val result = mutableListOf<MetadataDependencyResolution>()

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
            val selectedComponent = resolvedDependency.selected
            val componentId = selectedComponent.id

            if (!visitedDependencies.add(componentId)) {
                /* Already processed this dependency */
                continue
            }

            logger.debug("Transform dependency: $resolvedDependency")
            val dependencyResult = processDependency(
                resolvedDependency, parentSourceSetVisibilityProvider.getSourceSetsVisibleInParents(componentId)
            )
            logger.debug("Transformation result of dependency $resolvedDependency: $dependencyResult")

            result.add(dependencyResult)

            val transitiveDependenciesToVisit = when (dependencyResult) {
                is MetadataDependencyResolution.KeepOriginalDependency ->
                    selectedComponent.dependencies.filterIsInstance<ResolvedDependencyResult>()

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
        sourceSetsVisibleInParents: Set<String>,
    ): MetadataDependencyResolution {
        val module = dependency.selected
        val moduleId = module.id

        val compositeMetadataArtifact = params
            .resolvedMetadataConfiguration
            .getArtifacts(dependency)
            .singleOrNull()
            // Make sure that resolved metadata artifact is actually Multiplatform one
            ?.takeIf { it.variant.attributes.containsMultiplatformAttributes }
        // expected only ony Composite Metadata Klib, but if dependency got resolved into platform variant
        // when source set is a leaf then we might get multiple artifacts in such case we must return KeepOriginal
            ?: return MetadataDependencyResolution.KeepOriginalDependency(module)

        logger.debug("Transform composite metadata artifact: '${compositeMetadataArtifact.file}'")

        val mppDependencyMetadataExtractor = params.projectStructureMetadataExtractorFactory.create(compositeMetadataArtifact)
        val projectStructureMetadata = mppDependencyMetadataExtractor.getProjectStructureMetadata()
            ?: return MetadataDependencyResolution.KeepOriginalDependency(module)

        if (!projectStructureMetadata.isPublishedAsRoot) {
            error("Artifacts of dependency ${module.id.displayName} is built by old Kotlin Gradle Plugin and can't be consumed in this way")
        }

        val isResolvedToProject = moduleId is ProjectComponentIdentifier && moduleId.build.isCurrentBuild

        val sourceSetVisibility =
            params.sourceSetVisibilityProvider.getVisibleSourceSets(
                params.sourceSetName,
                dependency,
                projectStructureMetadata,
                isResolvedToProject
            )

        val allVisibleSourceSets = sourceSetVisibility.visibleSourceSetNames

        // Keep only the transitive dependencies requested by the visible source sets:
        // Visit the transitive dependencies visible by parents, too (i.e. allVisibleSourceSets), as this source set might get a more
        // concrete view on them:
        val requestedTransitiveDependencies: Set<ModuleDependencyIdentifier> =
            mutableSetOf<ModuleDependencyIdentifier>().apply {
                projectStructureMetadata.sourceSetModuleDependencies.forEach { (sourceSetName, moduleDependencies) ->
                    if (sourceSetName in allVisibleSourceSets) {
                        addAll(moduleDependencies)
                    }
                }
            }

        val transitiveDependenciesToVisit = module.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .filterTo(mutableSetOf()) { it.toModuleDependencyIdentifier() in requestedTransitiveDependencies }

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
                    moduleDependencyIdentifier = dependency.toModuleDependencyIdentifier(),
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

    /**
     * Behaves as [ModuleIds.fromComponent]
     */
    private fun ResolvedDependencyResult.toModuleDependencyIdentifier(): ModuleDependencyIdentifier {
        val component = selected
        return when (val componentId = component.id) {
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(componentId.group, componentId.module)
            is ProjectComponentIdentifier -> {
                if (componentId.build.isCurrentBuild) {
                    params.projectData[componentId.projectPath]?.moduleId?.get()
                        ?: error("Cant find project Module ID by ${componentId.projectPath}")
                } else {
                    ModuleDependencyIdentifier(
                        component.moduleVersion?.group ?: "unspecified",
                        component.moduleVersion?.name ?: "unspecified"
                    )
                }
            }

            else -> error("Unknown ComponentIdentifier: $this")
        }
    }

}

internal val ResolvedComponentResult.currentBuildProjectIdOrNull
    get(): ProjectComponentIdentifier? {
        val identifier = id
        return when {
            identifier is ProjectComponentIdentifier && identifier.build.isCurrentBuild -> identifier
            else -> null
        }
    }

private val Project.allProjectsData: Map<String, GranularMetadataTransformation.ProjectData> get() = extraProperties
    .getOrPut("all${GranularMetadataTransformation.ProjectData::class.java.simpleName}") { collectAllProjectsData() }

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

internal fun MetadataDependencyResolution.projectDependency(currentProject: Project): Project? =
    dependency.toProjectOrNull(currentProject)

internal fun ResolvedComponentResult.toProjectOrNull(currentProject: Project): Project? {
    val projectId = currentBuildProjectIdOrNull ?: return null
    return currentProject.project(projectId.projectPath)
}

private val KotlinMultiplatformExtension.platformCompilationSourceSets: Set<String>
    get() = targets.filterNot { it is KotlinMetadataTarget }
        .flatMap { target -> target.compilations }
        .flatMap { it.kotlinSourceSets }
        .map { it.name }
        .toSet()

internal val GranularMetadataTransformation?.metadataDependencyResolutionsOrEmpty get() = this?.metadataDependencyResolutions ?: emptyList()

internal val AttributeContainer.containsMultiplatformAttributes: Boolean
    get() = keySet().any { it.name == KotlinPlatformType.attribute.name }