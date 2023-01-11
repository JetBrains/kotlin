/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentSelector
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
import java.io.StringWriter
import java.util.*

internal sealed class MetadataDependencyResolution(
    val resolvedDependency: ResolvedDependencyResult,
) {
    val dependency: ResolvedComponentResult = resolvedDependency.selected
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
        resolvedDependency: ResolvedDependencyResult,
    ) : MetadataDependencyResolution(resolvedDependency)

    sealed class Exclude(
        resolvedDependency: ResolvedDependencyResult
    ) : MetadataDependencyResolution(resolvedDependency) {

        class Unrequested(
            resolvedDependency: ResolvedDependencyResult
        ) : Exclude(resolvedDependency)

        /**
         * Resolution for metadata dependencies of leaf platform source sets.
         * They are excluded since platform source sets should receive
         * platform dependencies from corresponding compilations and should not get metadata ones.
         * See KT-52216
         */
        class PublishedPlatformSourceSetDependency(
            resolvedDependency: ResolvedDependencyResult,
            val visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
        ) : Exclude(resolvedDependency)
    }

    class ChooseVisibleSourceSets internal constructor(
        resolvedDependency: ResolvedDependencyResult,
        val projectStructureMetadata: KotlinProjectStructureMetadata,
        val allVisibleSourceSetNames: Set<String>,
        val visibleSourceSetNamesExcludingDependsOn: Set<String>,
        val visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
        internal val metadataProvider: MetadataProvider
    ) : MetadataDependencyResolution(resolvedDependency) {

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

internal class MetadataDependencyResolutionSerializer(
    private val params: GranularMetadataTransformation.Params
) {
    private val resolvedMetadataConfiguration get() = params.resolvedMetadataConfiguration

    private val dependenciesByModuleId: Map<String, ResolvedDependencyResult> by lazy {
        resolvedMetadataConfiguration
            .allResolvedDependencies
            .mapNotNull {
                val id = it.selected.id
                when {
                    id is ModuleComponentIdentifier -> "${id.group}:${id.module}" to it
                    id is ProjectComponentIdentifier && !id.build.isCurrentBuild -> {
                        val requested = it.requested
                        if (requested is ModuleComponentSelector) {
                            "${requested.group}:${requested.module}" to it
                        } else {
                            null
                        }
                    }
                    else -> null
                }
            }.toMap()
    }

    private val dependenciesByProjectPath: Map<String, ResolvedDependencyResult> by lazy {
        resolvedMetadataConfiguration
            .allResolvedDependencies
            .mapNotNull {
                val id = it.selected.id
                if (id is ProjectComponentIdentifier && id.build.isCurrentBuild) {
                    id.projectPath to it
                } else {
                    null
                }
            }.toMap()
    }

    fun serializeList(resolutions: Iterable<MetadataDependencyResolution>): String {
        val stringWriter = StringWriter()
        val gson = GsonBuilder().setLenient().setPrettyPrinting().create()
        val writer = gson.newJsonWriter(stringWriter)
        writer.beginArray()
        resolutions.forEach { resolution -> writer.serialize(resolution) }
        writer.endArray()

        return stringWriter.toString()
    }

    fun parseList(string: String): List<MetadataDependencyResolution> {
        val json = JsonParser.parseString(string)

        return json.asJsonArray.map { it.asJsonObject.parseMetadataDependencyResolution() }
    }

    private fun JsonWriter.serialize(resolution: MetadataDependencyResolution) {
        when (resolution) {
            is MetadataDependencyResolution.ChooseVisibleSourceSets -> serialize(resolution)
            is MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency -> serialize(resolution)
            is MetadataDependencyResolution.Exclude.Unrequested -> serialize(resolution)
            is MetadataDependencyResolution.KeepOriginalDependency -> serialize(resolution)
        }
    }

    private fun JsonObject.parseMetadataDependencyResolution(): MetadataDependencyResolution {
        val type = get("type").asJsonPrimitive.asString
        return when (type) {
            "ChooseVisibleSourceSets" -> parseChooseVisibleSourceSets()
            "Exclude.PublishedPlatformSourceSetDependency" -> parseExcludePublishedPlatformSourceSetDependency()
            "Exclude.Unrequested" -> parseExcludeUnrequested()
            "KeepOriginalDependency" -> parseKeepOriginalDependency()
            else -> error("Unknown MetadataDependencyResolution type: '$type'")
        }
    }

    private fun JsonWriter.serialize(resolution: MetadataDependencyResolution.ChooseVisibleSourceSets) {
        beginObject()
        name("type"); value("ChooseVisibleSourceSets")
        name("dependencyId"); serialize(resolution.resolvedDependency)
        name("allVisibleSourceSetNames"); serializeStrings(resolution.allVisibleSourceSetNames)
        name("visibleSourceSetNamesExcludingDependsOn"); serializeStrings(resolution.visibleSourceSetNamesExcludingDependsOn)
        name("visibleTransitiveDependencies"); serialize(resolution.visibleTransitiveDependencies)
        endObject()
    }

    private fun ResolvedDependencyResult.toModuleDependencyIdentifier(): ModuleDependencyIdentifier {
        return when(val componentId = selected.id) {
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(componentId.group, componentId.module)
            is ProjectComponentIdentifier -> {
                if (componentId.build.isCurrentBuild) {
                    params.projectData[componentId.projectPath]?.moduleId?.get()
                        ?: error("Cant find project Module ID by ${componentId.projectPath}")
                } else {
                    when (val requestedModule = requested) {
                        is ProjectComponentSelector -> params.projectData[requestedModule.projectPath]?.moduleId?.get()
                            ?: error("Cant find project Module ID by ${requestedModule.projectPath}")
                        is ModuleComponentSelector -> ModuleDependencyIdentifier(requestedModule.group, requestedModule.module)
                        else -> error("Unknown ComponentSelector: '$requestedModule'")
                    }
                }
            }

            else -> error("Unknown ComponentIdentifier: $this")
        }
    }

    private fun JsonObject.parseChooseVisibleSourceSets(): MetadataDependencyResolution.ChooseVisibleSourceSets {
        val dependencyId = get("dependencyId").asJsonObject
        val dependency = lookupDependency(dependencyId)
        val module = dependency.selected
        val moduleId = module.id
        val artifact = resolvedMetadataConfiguration.dependencyArtifacts(dependency).single()

        val isResolvedToProject: Boolean = moduleId is ProjectComponentIdentifier && moduleId.build.isCurrentBuild

        val mppDependencyMetadataExtractor = params.projectStructureMetadataExtractorFactory.create(artifact)
        val projectStructureMetadata = mppDependencyMetadataExtractor.getProjectStructureMetadata()!!

        val sourceSetVisibility =
            params.sourceSetVisibilityProvider.getVisibleSourceSets(
                params.sourceSetName,
                dependency,
                projectStructureMetadata,
                isResolvedToProject
            )

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
            resolvedDependency = dependency,
            projectStructureMetadata = projectStructureMetadata,
            allVisibleSourceSetNames = get("allVisibleSourceSetNames")
                .asJsonArray.map { it.asJsonPrimitive.asString }.toSet(),
            visibleSourceSetNamesExcludingDependsOn = get("visibleSourceSetNamesExcludingDependsOn")
                .asJsonArray.map { it.asJsonPrimitive.asString }.toSet(),
            visibleTransitiveDependencies = get("visibleTransitiveDependencies")
                .asJsonArray.map { lookupDependency(it.asJsonObject) }.toSet(),
            metadataProvider = metadataProvider
        )
    }

    private fun JsonWriter.serialize(resolution: MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency) {
        beginObject()
        name("type"); value("Exclude.PublishedPlatformSourceSetDependency")
        name("dependencyId"); serialize(resolution.resolvedDependency)
        name("visibleTransitiveDependencies"); serialize(resolution.visibleTransitiveDependencies)
        endObject()
    }

    private fun JsonObject.parseExcludePublishedPlatformSourceSetDependency(): MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency {
        val dependencyId = get("dependencyId").asJsonObject
        val dependency = lookupDependency(dependencyId)

        return MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency(
            resolvedDependency = dependency,
            visibleTransitiveDependencies = get("visibleTransitiveDependencies")
                .asJsonArray
                .map { lookupDependency(it.asJsonObject) }
                .toSet()
        )
    }

    private fun JsonWriter.serialize(resolution: MetadataDependencyResolution.Exclude.Unrequested) {
        beginObject()
        name("type"); value("Exclude.Unrequested")
        name("dependencyId"); serialize(resolution.resolvedDependency)
        endObject()
    }

    private fun JsonObject.parseExcludeUnrequested(): MetadataDependencyResolution.Exclude.Unrequested {
        val dependencyId = get("dependencyId").asJsonObject
        val dependency = lookupDependency(dependencyId)

        return MetadataDependencyResolution.Exclude.Unrequested(dependency)
    }

    private fun JsonWriter.serialize(resolution: MetadataDependencyResolution.KeepOriginalDependency) {
        beginObject()
        name("type"); value("KeepOriginalDependency")
        name("dependencyId"); serialize(resolution.resolvedDependency)
        endObject()
    }

    private fun JsonObject.parseKeepOriginalDependency(): MetadataDependencyResolution.KeepOriginalDependency {
        val dependencyId = get("dependencyId").asJsonObject
        val dependency = lookupDependency(dependencyId)

        return MetadataDependencyResolution.KeepOriginalDependency(dependency)
    }

    private fun JsonWriter.serialize(component: ResolvedComponentResult) {
        val id = component.id
        when (id) {
            is ModuleComponentIdentifier -> serialize(id)
            is ProjectComponentIdentifier -> serialize(id)
            else -> error("Unknown Component ID: '$id'")
        }
    }

    private fun JsonWriter.serialize(dependency: ResolvedDependencyResult) {
        val id = dependency.selected.id
        when (id) {
            is ModuleComponentIdentifier -> serialize(id)
            is ProjectComponentIdentifier -> {
                if (id.build.isCurrentBuild) {
                    serialize(id)
                } else {
                    val selector = dependency.requested
                    when (selector) {
                        is ModuleComponentSelector -> serialize(selector)
                        is ProjectComponentSelector -> serialize(selector)
                        else -> error("Unknown selector '$selector'")
                    }
                }
            }
            else -> error("Unknown Component ID: '$id'")
        }
    }

    private fun JsonWriter.serialize(componentId: ModuleComponentIdentifier) {
        beginObject()
        name("type"); value("ModuleComponentIdentifier")
        name("group"); value(componentId.group)
        name("module"); value(componentId.module)
        endObject()
    }

    private fun JsonWriter.serialize(componentId: ProjectComponentIdentifier) {
        beginObject()
        name("type"); value("ProjectComponentIdentifier")
        name("projectPath"); value(componentId.projectPath)
        name("isCurrentBuild"); value(componentId.build.isCurrentBuild)
        endObject()
    }

    private fun JsonWriter.serialize(selector: ModuleComponentSelector) {
        beginObject()
        name("type"); value("ModuleComponentSelector")
        name("group"); value(selector.group)
        name("module"); value(selector.module)
        endObject()
    }

    private fun JsonWriter.serialize(selector: ProjectComponentSelector) {
        beginObject()
        name("type"); value("ProjectComponentSelector")
        name("projectPath"); value(selector.projectPath)
        endObject()
    }

    private fun JsonWriter.serializeStrings(strings: Iterable<String>) {
        beginArray()
        strings.forEach { string -> value(string) }
        endArray()
    }

    private fun JsonWriter.serialize(visibleTransitiveDependencies: Iterable<ResolvedDependencyResult>) {
        beginArray()
        visibleTransitiveDependencies.forEach { dependency -> serialize(dependency) }
        endArray()
    }

    private fun <T> JsonObject.lookupByDependency(
        lookupByModuleId: (ModuleDependencyIdentifier) -> T,
        lookupByProjectPath: (String) -> T
    ): T {
        val type = get("type").asJsonPrimitive.asString

        return when (type) {
            "ModuleComponentIdentifier", "ModuleComponentSelector" -> ModuleDependencyIdentifier(
                groupId = get("group").takeIf { !it.isJsonNull }?.asString,
                moduleId = get("module").asJsonPrimitive.asString,
            ).let(lookupByModuleId)
            "ProjectComponentIdentifier", "ProjectComponentSelector" -> lookupByProjectPath(get("projectPath").asJsonPrimitive.asString)
            else -> error("Unknown Dependency ID type: '$type'")
        }
    }

    private fun lookupDependency(dependencyId: JsonObject): ResolvedDependencyResult {
        return dependencyId.lookupByDependency(
            lookupByModuleId = { id -> dependenciesByModuleId["${id.groupId}:${id.moduleId}"] },
            lookupByProjectPath = { dependenciesByProjectPath[it] }
        ) ?: error("Cant find dependency by '$dependencyId'")
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

internal fun Iterable<MetadataDependencyResolution>.visibleSourceSets(): Map<String, Set<String>> = this
    .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
    .groupBy { it.dependency.id.displayName }
    .mapValues { (_, chooseVisibleSourceSets) -> chooseVisibleSourceSets.flatMapTo(mutableSetOf()) { it.allVisibleSourceSetNames } }

internal class GranularMetadataTransformation(
    val params: Params,
    /** A configuration that holds the dependencies of the appropriate scope for all Kotlin source sets in the project */
    private val parentVisibleSourceSets: Lazy<Map<String, Set<String>>>
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

    val ownVisibleSourceSets: Map<String, Set<String>> get() = metadataDependencyResolutions.visibleSourceSets()

    private fun ResolvedDependencyResult.toModuleDependencyIdentifier(): ModuleDependencyIdentifier {
        return when(val componentId = selected.id) {
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(componentId.group, componentId.module)
            is ProjectComponentIdentifier -> {
                if (componentId.build.isCurrentBuild) {
                    params.projectData[componentId.projectPath]?.moduleId?.get()
                        ?: error("Cant find project Module ID by ${componentId.projectPath}")
                } else {
                    when (val requestedModule = requested) {
                        is ProjectComponentSelector -> params.projectData[requestedModule.projectPath]?.moduleId?.get()
                            ?: error("Cant find project Module ID by ${requestedModule.projectPath}")
                        is ModuleComponentSelector -> ModuleDependencyIdentifier(requestedModule.group, requestedModule.module)
                        else -> error("Unknown ComponentSelector: '$requestedModule'")
                    }
                }
            }

            else -> error("Unknown ComponentIdentifier: $this")
        }
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
            val component = resolvedDependency.selected
            val componentId = component.id

            if (!visitedDependencies.add(componentId)) {
                /* Already processed this dependency */
                continue
            }

            val dependencyResult = processDependency(
                resolvedDependency,
                parentVisibleSourceSets.value[componentId.displayName].orEmpty()
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
                        resolvedDependency,
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
            .dependencyArtifacts(dependency)
            .singleOrNull()
            // expected only ony Composite Metadata Klib, but if dependency got resolved into platform variant
            // when source set is a leaf then we might get multiple artifacts in such case we must return KeepOriginal
            ?: return MetadataDependencyResolution.KeepOriginalDependency(dependency)

        val mppDependencyMetadataExtractor = params.projectStructureMetadataExtractorFactory.create(compositeMetadataArtifact)

        val isResolvedToProject: Boolean = moduleId is ProjectComponentIdentifier && moduleId.build.isCurrentBuild

        val projectStructureMetadata = mppDependencyMetadataExtractor.getProjectStructureMetadata()
            ?: return MetadataDependencyResolution.KeepOriginalDependency(dependency)

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
            .filterTo(mutableSetOf()) { it.toModuleDependencyIdentifier() in requestedTransitiveDependencies }

        if (params.sourceSetName in params.platformCompilationSourceSets && !isResolvedToProject)
            return MetadataDependencyResolution.Exclude.PublishedPlatformSourceSetDependency(dependency, transitiveDependenciesToVisit)

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
            resolvedDependency = dependency,
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