/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.artifacts.uklibStateAttribute
import org.jetbrains.kotlin.gradle.artifacts.uklibStateUnzipped
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Fragment
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Uklib
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.isSubsetOf
import org.jetbrains.kotlin.gradle.artifacts.uklibsPublication.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.PreparedKotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinProjectSharedDataProvider
import org.jetbrains.kotlin.gradle.plugin.internal.kotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.projectStructureMetadataResolvableConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File
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

    /**
     * KeepOriginalDependency нужен для кейса если в GMT попали настоящие jar'ники?
     */
    class KeepOriginalDependency(
        dependency: ResolvedComponentResult,
    ) : MetadataDependencyResolution(dependency)

    sealed class Exclude(
        dependency: ResolvedComponentResult,
    ) : MetadataDependencyResolution(dependency) {

        class Unrequested(
            dependency: ResolvedComponentResult,
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
        // FIXME: Why do we even have this?
        val projectStructureMetadata: KotlinProjectStructureMetadata?,
        val allVisibleSourceSetNames: Set<String>,
        val visibleSourceSetNamesExcludingDependsOn: Set<String>,
        val visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
        internal val metadataProvider: MetadataProvider,
    ) : MetadataDependencyResolution(dependency) {

        internal sealed class MetadataProvider {
            class ArtifactMetadataProvider(private val compositeMetadataArtifact: CompositeMetadataArtifact) :
                MetadataProvider(), CompositeMetadataArtifact by compositeMetadataArtifact

            abstract class ProjectMetadataProvider : MetadataProvider() {
                abstract fun getSourceSetCompiledMetadata(sourceSetName: String): FileCollection?
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
    val parentSourceSetVisibilityProvider: ParentSourceSetVisibilityProvider,
    val kotlinToolingDiagnosticsCollector: PreparedKotlinToolingDiagnosticsCollector,
) {
    private val logger = Logging.getLogger("GranularMetadataTransformation[${params.sourceSetName}]")

    class Params(
        val build: CurrentBuildIdentifier,
        val sourceSetName: String,
        val resolvedMetadataConfiguration: LazyResolvedConfiguration,
        val sourceSetVisibilityProvider: SourceSetVisibilityProvider,
        val projectStructureMetadataExtractorFactory: IMppDependenciesProjectStructureMetadataExtractorFactory,
        val projectData: Map<String, ProjectData>,
        val platformCompilationSourceSets: Set<String>,
        val projectStructureMetadataResolvableConfiguration: LazyResolvedConfiguration?,
        val objects: ObjectFactory,
        val kotlinKmpProjectIsolationEnabled: Boolean,
        val sourceSetMetadataLocationsOfProjectDependencies: KotlinProjectSharedDataProvider<SourceSetMetadataLocations>,
        val uklibFragmentAttributes: Set<String>,
    ) {
        constructor(project: Project, kotlinSourceSet: KotlinSourceSet) : this(
            build = project.currentBuild,
            sourceSetName = kotlinSourceSet.name,
            resolvedMetadataConfiguration = LazyResolvedConfiguration(kotlinSourceSet.internal.resolvableMetadataConfiguration),
            sourceSetVisibilityProvider = SourceSetVisibilityProvider(project),
            projectStructureMetadataExtractorFactory = if (project.kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled) project.kotlinMppDependencyProjectStructureMetadataExtractorFactory else project.kotlinMppDependencyProjectStructureMetadataExtractorFactoryDeprecated,
            projectData = if (project.kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled) emptyMap<String, ProjectData>() else project.allProjectsData,
            platformCompilationSourceSets = project.multiplatformExtension.platformCompilationSourceSets,
            projectStructureMetadataResolvableConfiguration =
                kotlinSourceSet.internal.projectStructureMetadataResolvableConfiguration?.let { LazyResolvedConfiguration(it) },
            objects = project.objects,
            kotlinKmpProjectIsolationEnabled = project.kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled,
            sourceSetMetadataLocationsOfProjectDependencies = project.kotlinSecondaryVariantsDataSharing
                .consumeCommonSourceSetMetadataLocations(kotlinSourceSet.internal.resolvableMetadataConfiguration),
            uklibFragmentAttributes = kotlinSourceSet.internal.compilations
                .map { it.target }
                .filter { it !is KotlinMetadataTarget }
                .map { it.uklibFragmentPlatformAttribute.unwrap() }
                .toSet()
        )
    }

    class ProjectData(
        val path: String,
        val sourceSetMetadataOutputs: LenientFuture<Map<String, SourceSetMetadataOutputs>>,
    ) {
        override fun toString(): String = "ProjectData[path='$path']"
    }

    val metadataDependencyResolutions: Iterable<MetadataDependencyResolution> by lazy { doTransform() }

    val visibleSourceSetsByComponentId: Map<ComponentIdentifier, Set<String>> by lazy {
        metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .groupBy { it.dependency.id }
            .mapValues { (_, visibleSourceSets) -> visibleSourceSets.flatMap { it.allVisibleSourceSetNames }.toSet() }
    }

    private fun doTransform(): Iterable<MetadataDependencyResolution> {
        val result = mutableListOf<MetadataDependencyResolution>()

        // Это очередь из порезолвленных компонентнов?
        val resolvedDependencyQueue: Queue<ResolvedDependencyResult> = ArrayDeque<ResolvedDependencyResult>().apply {
            addAll(
                params.resolvedMetadataConfiguration
                    .root
                    .dependencies
                    .filter { !it.isConstraint }
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
                transitiveDependenciesToVisit
                    .filter { it.selected.id !in visitedDependencies }
                    .filter { !it.isConstraint }
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
    // По идее эта логика исполняется на зависимость, на каждый потребляющий SS
    private fun processDependency(
        dependency: ResolvedDependencyResult,
        // Я не понимаю откуда приходят эти SS
        /**
         * 24.11.2024 - Наверное для iosMain они приходят из commonMain. Это точно так, они приходят из parent тасок которые делают GMT
         */
        sourceSetsVisibleInParents: Set<String>,
    ): MetadataDependencyResolution {
        val module = dependency.selected
        val moduleId = module.id

        /**
         * FIXME: 01.12-2024 - Add diagnostics:
         *
         * 1. Look at the graph of
         */

        // Это .jar ник с метадатными klib'ами
        val artifact = params
            .resolvedMetadataConfiguration
            .getArtifacts(dependency)
            // FIXME: Check this is correct?
            .singleOrNull() ?: return MetadataDependencyResolution.KeepOriginalDependency(module)
        // expected only Composite Metadata Klib, but if dependency got resolved into platform variant
        // when a source set is a leaf, then we might get multiple artifacts in such a case we must return KeepOriginal

        // Make sure that resolved metadata artifact is actually Multiplatform one
        if (artifact.variant.attributes.containsMultiplatformMetadataAttributes) {
            return processPSMDependency(
                artifact,
                dependency,
                module,
                moduleId,
                sourceSetsVisibleInParents
            )
        } else if (artifact.variant.attributes.getAttribute(uklibStateAttribute) == uklibStateUnzipped) {
            return processUklibDependency(
                artifact,
                dependency,
                module,
                moduleId,
                sourceSetsVisibleInParents,

                //sourceSetName = params.sourceSetName,
                uklibFragmentAttributes = params.uklibFragmentAttributes,
            )
        } else {
            return MetadataDependencyResolution.KeepOriginalDependency(module)
        }
    }

    // FIXME: How do we ensure classpath ordering for PSM dependencies ???
    private fun processUklibDependency(
        compositeMetadataArtifact: ResolvedArtifactResult,
        dependency: ResolvedDependencyResult,
        module: ResolvedComponentResult,
        moduleId: ComponentIdentifier,

        sourceSetsVisibleInParents: Set<String>,
        uklibFragmentAttributes: Set<String>,
    ): MetadataDependencyResolution {
        // Validate that

        val uklibDependency = Uklib.deserializeFromDirectory(
            compositeMetadataArtifact.file,
        )

        val fragmentCanSee: Fragment.(Fragment) -> Boolean = { attributes.isSubsetOf(it.attributes) }
        val visibleFragments = uklibDependency.module.fragments.filter {
            uklibFragmentAttributes.isSubsetOf(it.attributes)
                    // FIXME: Is this the correct place to filter out compilations transformed by the parent ?? See allVisibleSourceSetNames and visibleSourceSetNamesExcludingDependsOn
                    && it.identifier !in sourceSetsVisibleInParents
        }.sortedWith(
            object : Comparator<Fragment> {
                override fun compare(left: Fragment, right: Fragment): Int {
                    if (left.fragmentCanSee(right)) {
                        return -1
                    } else if (right.fragmentCanSee(left)) {
                        return 1
                    } else if (left.attributes == right.attributes) {
                        return 0
                    } else {
                        return left.identifier.compareTo(right.identifier)
                    }
                }
            }
        )

        val moduleVersion = dependency.selected.moduleVersion!!

        return MetadataDependencyResolution.ChooseVisibleSourceSets(
            dependency = dependency.selected,
            projectStructureMetadata = null,
            // FIXME: Don't filter this
            allVisibleSourceSetNames = visibleFragments.map { it.identifier }.toHashSet(),
            // FIXME: Only filter this, but for some reason this doesn't work???
            visibleSourceSetNamesExcludingDependsOn = visibleFragments.map { it.identifier }.toHashSet(),

            // 26.11.2024 - This is likely only used to walk further dependencies, so we want all of them
            visibleTransitiveDependencies = dependency.selected.dependencies.filterIsInstance<ResolvedDependencyResult>().toHashSet(),
            metadataProvider = ArtifactMetadataProvider(
                object : CompositeMetadataArtifact {

                    override val moduleDependencyIdentifier: ModuleDependencyIdentifier
                        get() = ModuleDependencyIdentifier(moduleVersion.group, moduleVersion.name)
                    override val moduleDependencyVersion: String
                        get() = moduleVersion.version

                    override fun open(): CompositeMetadataArtifactContent {
                        val backrefArtifact = this
                        return object : CompositeMetadataArtifactContent {
                            val backrefContent = this
                            private val fragmentSourceSets: Map<String, CompositeMetadataArtifactContent.SourceSetContent> = visibleFragments.keysToMap { fragment ->
                                object : CompositeMetadataArtifactContent.SourceSetContent {
                                    override val containingArtifactContent: CompositeMetadataArtifactContent
                                        get() = backrefContent
                                    override val sourceSetName: String
                                        get() = fragment.identifier
                                    override val metadataBinary: CompositeMetadataArtifactContent.MetadataBinary?
                                        // FIXME: ???
                                        get()  {
                                            val backref = this
                                            return object : CompositeMetadataArtifactContent.MetadataBinary {
                                                override val containingSourceSetContent: CompositeMetadataArtifactContent.SourceSetContent
                                                    get() = backref
                                                override val archiveExtension: String
                                                    get() = ""
                                                // This needs to be unique per fragment
                                                override val relativeFile: File
                                                    get() = File("uklib-${moduleVersion.group}-${moduleVersion.name}-${moduleVersion.version}-${fragment.identifier}")
                                                override val checksum: String
                                                    // FIXME: Why does this even exist?
                                                    get() = ""

                                                override fun copyTo(file: File): Boolean {
                                                    return fragment.file().copyRecursively(
                                                        file,
                                                        overwrite = true,
                                                    )
                                                }
                                            }
                                        }
                                    override val cinteropMetadataBinaries: List<CompositeMetadataArtifactContent.CInteropMetadataBinary>
                                        get() = emptyList()

                                }
                            }.mapKeys { it.key.identifier }

                            override val containingArtifact: CompositeMetadataArtifact
                                get() = backrefArtifact
                            override val sourceSets: List<CompositeMetadataArtifactContent.SourceSetContent>
                                get() = fragmentSourceSets.values.toList()

                            override fun getSourceSet(name: String): CompositeMetadataArtifactContent.SourceSetContent {
                                return fragmentSourceSets[name]!!
                            }

                            override fun findSourceSet(name: String): CompositeMetadataArtifactContent.SourceSetContent? {
                                return fragmentSourceSets[name]
                            }

                            override fun close() {
                                // ???
                            }
                        }
                    }

                    override fun exists(): Boolean = true
                }
            )
        )
    }

    private fun processPSMDependency(
        compositeMetadataArtifact: ResolvedArtifactResult,
        dependency: ResolvedDependencyResult,
        module: ResolvedComponentResult,
        moduleId: ComponentIdentifier,
        sourceSetsVisibleInParents: Set<String>,
    ): MetadataDependencyResolution {
        logger.debug("Transform composite metadata artifact: '${compositeMetadataArtifact.file}'")

        val mppDependencyMetadataExtractor =
            params.projectStructureMetadataExtractorFactory.create(
                compositeMetadataArtifact,
                dependency,
                kotlinToolingDiagnosticsCollector,
                params.projectStructureMetadataResolvableConfiguration
            ) ?: return MetadataDependencyResolution.KeepOriginalDependency(module)

        val projectStructureMetadata = mppDependencyMetadataExtractor.getProjectStructureMetadata()
            ?: return MetadataDependencyResolution.KeepOriginalDependency(module)

        if (!projectStructureMetadata.isPublishedAsRoot) {
            error("Artifacts of dependency ${module.id.displayName} is built by old Kotlin Gradle Plugin and can't be consumed in this way")
        }

        val isResolvedToProject = moduleId in params.build

        val sourceSetVisibility =
            params.sourceSetVisibilityProvider.getVisibleSourceSets(
                params.sourceSetName,
                dependency,
                projectStructureMetadata,
                isResolvedToProject
            )

        // Тут будут те SS которые видит потребитель? Но где тогда происходит маппинг между SS?
        /**
         * 24.11.2024 - Все это происходит в рамках resolve'а classpath текущего SS
         */
        val allVisibleSourceSets = sourceSetVisibility.visibleSourceSetNames

        // Keep only the transitive dependencies requested by the visible source sets:
        // Visit the transitive dependencies visible by parents, too (i.e. allVisibleSourceSets), as this source set might get a more
        // concrete view on them:
        /**
         * Набираем то, какие зависимости были указаны в рамках SS
         */
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

        /**
         * Кажется мы проверяем, что SS в котором мы сейчас не платформенный и что он не порезолвился в проектную зависимость
         *
         * Не понимаю зачем это нужно, особенно как работает часть про !isResolvedToProject
         */
        if (params.sourceSetName in params.platformCompilationSourceSets && !isResolvedToProject)
            return MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency(module, transitiveDependenciesToVisit)

        // Наверное тут будут только новые видимые SS которые еще не были видны в parent SS
        val visibleSourceSetsExcludingDependsOn = allVisibleSourceSets.filterTo(mutableSetOf()) { it !in sourceSetsVisibleInParents }

        val metadataProvider = when (mppDependencyMetadataExtractor) {
            is AbstractProjectMppDependencyProjectStructureMetadataExtractor -> {
                val sourceSetMetadataOutputs = extractSourceSetMetadataOutputs(
                    allVisibleSourceSets,
                    mppDependencyMetadataExtractor,
                    dependency
                )
                ProjectMetadataProvider(
                    sourceSetMetadataOutputs = sourceSetMetadataOutputs
                )
            }
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

    private fun extractSourceSetMetadataOutputs(
        visibleSourceSetNames: Set<String>,
        mppDependencyMetadataExtractor: AbstractProjectMppDependencyProjectStructureMetadataExtractor,
        dependency: ResolvedDependencyResult,
    ): Map<String, SourceSetMetadataOutputs> {
        return if (params.kotlinKmpProjectIsolationEnabled) {
            val sourceSetMetadataLocations = params
                .sourceSetMetadataLocationsOfProjectDependencies
                .getProjectDataFromDependencyOrNull(dependency)
            // TODO: introduce IDE mode for GranularMetadataTransformations, in this mode GMT
            // will not materialize actual metadata artifacts for Project Dependencies,
            // so here we could simply return, or even don't invoke this method.
            // but for now just fallback to "null" provider.
            if (sourceSetMetadataLocations == null) {
                visibleSourceSetNames.associateWith { SourceSetMetadataOutputs(null) }
            } else {
                sourceSetMetadataLocations.locationBySourceSetName.mapValues { (_, classDir) ->
                    SourceSetMetadataOutputs(params.objects.fileCollection().from(classDir))
                }
            }
        } else {
            params.projectData[mppDependencyMetadataExtractor.projectPath]?.sourceSetMetadataOutputs
                ?.getOrThrow() ?: error("Unexpected project path '${mppDependencyMetadataExtractor.projectPath}'")
        }
    }

    /**
     * Behaves as [ModuleIds.fromComponent]
     */
    private fun ResolvedDependencyResult.toModuleDependencyIdentifier(): ModuleDependencyIdentifier {
        val component = selected
        return when (val componentId = component.id) {
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(componentId.group, componentId.module)
            is ProjectComponentIdentifier -> {
                if (componentId in params.build) {
                    ModuleDependencyIdentifier(component.moduleVersion?.group, componentId.projectName)
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

private val Project.allProjectsData: Map<String, GranularMetadataTransformation.ProjectData> by projectStoredProperty {
    collectAllProjectsData()
}

private fun Project.collectAllProjectsData(): Map<String, GranularMetadataTransformation.ProjectData> {
    return rootProject.allprojects.associateBy { it.path }.mapValues { (path, currentProject) ->

        GranularMetadataTransformation.ProjectData(
            path = path,
            sourceSetMetadataOutputs = currentProject.future { currentProject.collectSourceSetMetadataOutputs() }.lenient,
        )
    }
}

internal fun MetadataDependencyResolution.projectDependency(currentProject: Project): Project? =
    dependency.toProjectOrNull(currentProject)

internal fun ResolvedComponentResult.toProjectOrNull(currentProject: Project): Project? {
    if (this !in currentProject.currentBuild) return null
    val projectId = id as? ProjectComponentIdentifier ?: return null
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

private val AttributeContainer.containsMultiplatformMetadataAttributes: Boolean
    get() = keySet().any { it.name == KotlinPlatformType.attribute.name && getAttribute(it).toString() == KotlinPlatformType.common.name }