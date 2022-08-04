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

//
//internal sealed class MetadataDependencyResolution2(
//    @field:Transient // can't be used with Gradle Instant Execution, but fortunately not needed when deserialized
//    val dependency: ResolvedComponentResult,
//    @field:Transient
//    val projectDependency: ProjectId?
//) {
//    /** Evaluate and store the value, as the [dependency] will be lost during Gradle instant execution */
////    val originalArtifactFiles: List<File> = dependency.dependents.flatMap {  it.allModuleArtifacts } .map { it.file }
//
//    override fun toString(): String {
//        val verb = when (this) {
//            is KeepOriginalDependency -> "keep"
//            is Exclude -> "exclude"
//            is ChooseVisibleSourceSets -> "choose"
//        }
//        return "$verb, dependency = $dependency"
//    }
//
//    class KeepOriginalDependency(
//        dependency: ResolvedComponentResult,
//        projectDependency: ProjectId?
//    ) : MetadataDependencyResolution2(dependency, projectDependency)
//
//    sealed class Exclude(
//        dependency: ResolvedComponentResult,
//        projectDependency: ProjectId?
//    ) : MetadataDependencyResolution2(dependency, projectDependency) {
//
//        class Unrequested(
//            dependency: ResolvedComponentResult,
//            projectDependency: ProjectId?
//        ) : Exclude(dependency, projectDependency)
//
//        /**
//         * Resolution for metadata dependencies of leaf platform source sets.
//         * They are excluded since platform source sets should receive
//         * platform dependencies from corresponding compilations and should not get metadata ones.
//         * See KT-52216
//         */
//        class PublishedPlatformSourceSetDependency(
//            dependency: ResolvedComponentResult,
//            val visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
//        ) : Exclude(dependency, null)
//    }
//
//    class ChooseVisibleSourceSets internal constructor(
//        dependency: ResolvedComponentResult,
//        projectDependency: ProjectId?,
//        val projectStructureMetadata: KotlinProjectStructureMetadata,
//        val allVisibleSourceSetNames: Set<String>,
//        val visibleSourceSetNamesExcludingDependsOn: Set<String>,
//        val visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
//        internal val metadataProvider: MetadataProvider
//    ) : MetadataDependencyResolution2(dependency, projectDependency) {
//
//        internal sealed class MetadataProvider {
//            class JarMetadataProvider(private val compositeMetadataArtifact: CompositeMetadataJar) :
//                MetadataProvider(), CompositeMetadataJar by compositeMetadataArtifact
//
//            abstract class ProjectMetadataProvider : MetadataProvider() {
//                enum class MetadataConsumer { Ide, Cli }
//
//                abstract fun getSourceSetCompiledMetadata(sourceSetName: String): FileCollection
//                abstract fun getSourceSetCInteropMetadata(sourceSetName: String, consumer: MetadataConsumer): FileCollection
//            }
//
//            companion object {
//                fun CompositeMetadataJar.asMetadataProvider() = JarMetadataProvider(this)
//            }
//        }
//
//        override fun toString(): String =
//            super.toString() + ", sourceSets = " + allVisibleSourceSetNames.joinToString(", ", "[", "]") {
//                (if (it in visibleSourceSetNamesExcludingDependsOn) "*" else "") + it
//            }
//    }
//}
//
//internal class GranularMetadataTransformation2(
//    private val projectData: Map<String, ProjectData>,
//    val kotlinSourceSet: KotlinSourceSet,
//    val allRequestedDependencies: List<DependencyId>,
//    /** A list of scopes that the dependencies from [kotlinSourceSet] are treated as requested dependencies. */
//    private val sourceSetRequestedScopes: List<KotlinDependencyScope>,
//    private val resolvedDependencies: ResolvedDependencyGraph,
//    private val psmExtractor: MppDependencyProjectStructureMetadataExtractor2,
//    // private val resolvedDependencies: Map<KotlinDependencyScope, ResolvedDependencyGraph>,
//    /** A configuration that holds the dependencies of the appropriate scope for all Kotlin source sets in the project */
//    private val parentTransformations: Lazy<Iterable<GranularMetadataTransformation2>>
//) {
//
//    private val moduleIds = ModuleIds2(projectData)
//
//    val metadataDependencyResolutions: Iterable<MetadataDependencyResolution2> by lazy { doTransform() }
//
//    // Keep parents of each dependency, too. We need a dependency's parent when it's an MPP's metadata module dependency:
//    // in this case, the parent is the MPP's root module.
//    private data class ResolvedDependencyWithParent(
//        val dependency: ResolvedComponentResult,
//        val parent: ResolvedComponentResult?
//    )
//
//
////    private val requestedDependencies: Iterable<Dependency> by lazy {
////        requestedDependencies(project, kotlinSourceSet, sourceSetRequestedScopes)
////    }
////
////    private val allSourceSetsConfiguration: Configuration =
////        commonMetadataDependenciesConfigurationForScopes(project, sourceSetRequestedScopes)
////
////    internal val configurationToResolve: Configuration by lazy {
////        resolvableMetadataConfiguration(project, allSourceSetsConfiguration, requestedDependencies)
////    }
//
//    private fun doTransform(): Iterable<MetadataDependencyResolution2> {
//        val result = mutableListOf<MetadataDependencyResolution2>()
//
//        val parentResolutions =
//            parentTransformations.value.flatMap { it.metadataDependencyResolutions }.groupBy {
//                moduleIds.fromComponent(it.dependency)
//            }
//
//        val resolvedComponentResult = resolvedDependencies.root
//        val allModuleDependencies = resolvedDependencies.allDependencies.filterIsInstance<ResolvedDependencyResult>()
//
//        val resolvedDependencyQueue: Queue<ResolvedDependencyWithParent> = ArrayDeque<ResolvedDependencyWithParent>().apply {
//            val requestedModules: Set<ModuleDependencyIdentifier> = allRequestedDependencies.mapTo(mutableSetOf()) {
//                moduleIds.fromDependency(it)
//            }
//
//            addAll(
//                resolvedComponentResult.dependencies
//                    .filter { moduleIds.fromComponentSelector(it.requested) in requestedModules }
//                    .filterIsInstance<ResolvedDependencyResult>()
//                    .map { ResolvedDependencyWithParent(it.selected, null) }
//            )
//        }
//
//        val visitedDependencies = mutableSetOf<ResolvedComponentResult>()
//
//        while (resolvedDependencyQueue.isNotEmpty()) {
//            val (resolvedDependency: ResolvedComponentResult, parent: ResolvedComponentResult?) = resolvedDependencyQueue.poll()
//
//            if (!visitedDependencies.add(resolvedDependency)) {
//                /* Already processed this dependency */
//                continue
//            }
//
//            val dependencyResult = processDependency(
//                resolvedDependency,
//                parentResolutions[moduleIds.fromComponent(resolvedDependency)].orEmpty(),
//                parent
//            )
//
//            result.add(dependencyResult)
//
//            val transitiveDependenciesToVisit = when (dependencyResult) {
//                is MetadataDependencyResolution2.KeepOriginalDependency ->
//                    resolvedDependency.dependencies.filterIsInstance<ResolvedDependencyResult>()
//                is MetadataDependencyResolution2.ChooseVisibleSourceSets -> dependencyResult.visibleTransitiveDependencies
//                is MetadataDependencyResolution2.Exclude.PublishedPlatformSourceSetDependency -> dependencyResult.visibleTransitiveDependencies
//                is MetadataDependencyResolution2.Exclude.Unrequested -> error("a visited dependency is erroneously considered unrequested")
//            }
//
//            resolvedDependencyQueue.addAll(
//                transitiveDependenciesToVisit.filter { it.selected !in visitedDependencies }
//                    .map { ResolvedDependencyWithParent(it.selected, resolvedDependency) }
//            )
//        }
//
//        allModuleDependencies.forEach { resolvedDependency ->
//            if (resolvedDependency.selected !in visitedDependencies) {
//                val projectDependency = resolvedDependency.selected.id as? ProjectComponentIdentifier
//                result.add(
//                    MetadataDependencyResolution2.Exclude.Unrequested(
//                        resolvedDependency.selected,
//                        if (projectDependency != null && projectDependency.build.isCurrentBuild) {
//                            ProjectId(projectDependency.projectPath, projectDependency.build.name)
//                        } else {
//                            null
//                        }
//                    )
//                )
//            }
//        }
//
//        return result
//    }
//
//    /**
//     * If the [module] is an MPP metadata module, we extract [KotlinProjectStructureMetadata] and do the following:
//     *
//     * * get the [KotlinProjectStructureMetadata] from the dependency (either deserialize from the artifact or build from the project)
//     *
//     * * determine the set *S* of source sets that should be seen in the [kotlinSourceSet] by finding which variants the [parent]
//     *   dependency got resolved for the compilations where [kotlinSourceSet] participates:
//     *
//     * * transform the single Kotlin metadata artifact into a set of Kotlin metadata artifacts for the particular source sets in
//     *   *S* and add the results as [MetadataDependencyResolution.ChooseVisibleSourceSets]
//     *
//     * * based on the project structure metadata, determine which of the module's dependencies are requested by the
//     *   source sets in *S*, then consider only these transitive dependencies, ignore the others;
//     */
//    private fun processDependency(
//        module: ResolvedComponentResult,
//        parentResolutionsForModule: Iterable<MetadataDependencyResolution2>,
//        parent: ResolvedComponentResult?
//    ): MetadataDependencyResolution2 {
////        val mppDependencyMetadataExtractor = MppDependencyProjectStructureMetadataExtractor.create(
////            project, module, configurationToResolve,
////            resolveViaAvailableAt = false // we will process the available-at module as a dependency later in the queue
////        )
////
////        val resolvedToProject: Project? = module.toProjectOrNull(project)
////
////        val projectStructureMetadata = mppDependencyMetadataExtractor?.getProjectStructureMetadata()
////            ?: return MetadataDependencyResolution.KeepOriginalDependency(module, resolvedToProject)
//
//        val projectStructureMetadata = psmExtractor.extractFromComponent(module)
//
//        val sourceSetVisibility =
//            SourceSetVisibilityProvider2(project).getVisibleSourceSets(
//                kotlinSourceSet,
//                sourceSetRequestedScopes,
//                if (projectStructureMetadata.isPublishedAsRoot) module else parent, module,
//                projectStructureMetadata,
//                resolvedToProject
//            )
//
//        val allVisibleSourceSets = sourceSetVisibility.visibleSourceSetNames
//
//        val sourceSetsVisibleInParents = parentResolutionsForModule
//            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
//            .flatMapTo(mutableSetOf()) { it.allVisibleSourceSetNames }
//
//        // Keep only the transitive dependencies requested by the visible source sets:
//        // Visit the transitive dependencies visible by parents, too (i.e. allVisibleSourceSets), as this source set might get a more
//        // concrete view on them:
//        val requestedTransitiveDependencies: Set<ModuleDependencyIdentifier> =
//            mutableSetOf<ModuleDependencyIdentifier>().apply {
//                projectStructureMetadata.sourceSetModuleDependencies.forEach { (sourceSetName, moduleDependencies) ->
//                    if (sourceSetName in allVisibleSourceSets) {
//                        addAll(moduleDependencies.map { ModuleDependencyIdentifier(it.groupId, it.moduleId) })
//                    }
//                }
//            }
//
//        val transitiveDependenciesToVisit = module.dependencies
//            .filterIsInstance<ResolvedDependencyResult>()
//            .filterTo(mutableSetOf()) { ModuleIds.fromComponent(project, it.selected) in requestedTransitiveDependencies }
//
//        if (kotlinSourceSet in project.multiplatformExtension.platformCompilationSourceSets && resolvedToProject == null)
//            return MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency(module, transitiveDependenciesToVisit)
//
//        val visibleSourceSetsExcludingDependsOn = allVisibleSourceSets.filterTo(mutableSetOf()) { it !in sourceSetsVisibleInParents }
//
//        val metadataProvider = when (mppDependencyMetadataExtractor) {
//            is ProjectMppDependencyProjectStructureMetadataExtractor -> ProjectMetadataProvider(
//                dependencyProject = mppDependencyMetadataExtractor.dependencyProject,
//                moduleIdentifier = mppDependencyMetadataExtractor.moduleIdentifier
//            )
//
//            is JarMppDependencyProjectStructureMetadataExtractor -> CompositeMetadataJar(
//                moduleIdentifier = ModuleIds.fromComponent(project, module).toString(),
//                projectStructureMetadata = projectStructureMetadata,
//                primaryArtifactFile = mppDependencyMetadataExtractor.primaryArtifactFile,
//                hostSpecificArtifactsBySourceSet = sourceSetVisibility.hostSpecificMetadataArtifactBySourceSet,
//            ).asMetadataProvider()
//        }
//
//        return MetadataDependencyResolution.ChooseVisibleSourceSets(
//            dependency = module,
//            projectDependency = resolvedToProject,
//            projectStructureMetadata = projectStructureMetadata,
//            allVisibleSourceSetNames = allVisibleSourceSets,
//            visibleSourceSetNamesExcludingDependsOn = visibleSourceSetsExcludingDependsOn,
//            visibleTransitiveDependencies = transitiveDependenciesToVisit,
//            metadataProvider = metadataProvider
//        )
//    }
//}
//
//internal fun ResolvedComponentResult.toProjectOrNull(currentProject: Project): Project? {
//    val identifier = id
//    return when {
//        identifier is ProjectComponentIdentifier && identifier.build.isCurrentBuild -> currentProject.project(identifier.projectPath)
//        else -> null
//    }
//}
//
//internal fun resolvableMetadataConfiguration(
//    project: Project,
//    sourceSets: Iterable<KotlinSourceSet>,
//    scopes: Iterable<KotlinDependencyScope>
//) = resolvableMetadataConfiguration(
//    project,
//    commonMetadataDependenciesConfigurationForScopes(project, scopes),
//    sourceSets.flatMapTo(mutableListOf()) { requestedDependencies(project, it, scopes) }
//)
//
///** If a source set is not a published source set, its dependencies are not included in [allSourceSetsConfiguration].
// * In that case, to resolve the dependencies of the source set in a way that is consistent with the published source sets,
// * we need to create a new configuration with the dependencies from both [allSourceSetsConfiguration] and the
// * other [requestedDependencies] */
//// TODO: optimize by caching the resulting configurations?
//internal fun resolvableMetadataConfiguration(
//    project: Project,
//    allSourceSetsConfiguration: Configuration,
//    requestedDependencies: Iterable<Dependency>
//): Configuration {
//    var modifiedConfiguration: Configuration? = null
//
//    val originalDependencies = allSourceSetsConfiguration.allDependencies
//
//    requestedDependencies.forEach { dependency ->
//        if (dependency !in originalDependencies) {
//            modifiedConfiguration = modifiedConfiguration ?: project.configurations.detachedConfiguration().apply {
//                fun <T> copyAttribute(key: Attribute<T>) {
//                    attributes.attribute(key, allSourceSetsConfiguration.attributes.getAttribute(key)!!)
//                }
//                allSourceSetsConfiguration.attributes.keySet().forEach { copyAttribute(it) }
//                dependencies.addAll(originalDependencies)
//            }
//            modifiedConfiguration!!.dependencies.add(dependency)
//        }
//    }
//    return modifiedConfiguration ?: allSourceSetsConfiguration
//}
//
///** The configuration that contains the dependencies of the corresponding scopes (and maybe others)
// * from all published source sets. */
//internal fun commonMetadataDependenciesConfigurationForScopes(
//    project: Project,
//    scopes: Iterable<KotlinDependencyScope>
//): Configuration {
//    // TODO: what if 'runtimeOnly' is combined with 'compileOnly'? prohibit this or merge the two? we never do that now, though
//    val configurationName = if (KotlinDependencyScope.RUNTIME_ONLY_SCOPE in scopes)
//        ALL_RUNTIME_METADATA_CONFIGURATION_NAME
//    else
//        ALL_COMPILE_METADATA_CONFIGURATION_NAME
//    return project.configurations.getByName(configurationName)
//}
//
//internal fun requestedDependencies(
//    project: Project,
//    sourceSet: KotlinSourceSet,
//    requestedScopes: Iterable<KotlinDependencyScope>
//): Iterable<Dependency> {
//    fun collectScopedDependenciesFromSourceSet(sourceSet: KotlinSourceSet): Set<Dependency> =
//        requestedScopes.flatMapTo(mutableSetOf()) { scope ->
//            project.sourceSetDependencyConfigurationByScope(sourceSet, scope).incoming.dependencies
//        }
//
//    val otherContributingSourceSets = dependsOnClosureWithInterCompilationDependencies(project, sourceSet)
//    return listOf(sourceSet, *otherContributingSourceSets.toTypedArray()).flatMap(::collectScopedDependenciesFromSourceSet)
//}
//
//private val KotlinMultiplatformExtension.platformCompilationSourceSets: Set<KotlinSourceSet>
//    get() = targets.filterNot { it is KotlinMetadataTarget }
//        .flatMap { target -> target.compilations }
//        .flatMap { it.kotlinSourceSetsIncludingDefault }
//        .toSet()
