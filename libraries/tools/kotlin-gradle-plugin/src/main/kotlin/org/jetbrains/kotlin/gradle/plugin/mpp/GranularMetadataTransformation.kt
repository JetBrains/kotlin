/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toSingleModuleIdentifier
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_RUNTIME_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

internal sealed class MetadataDependencyResolution(
    @field:Transient // can't be used with Gradle Instant Execution, but fortunately not needed when deserialized
    val dependency: ResolvedComponentResult,
    @field:Transient
    val projectDependency: Project?
) {
    /** Evaluate and store the value, as the [dependency] will be lost during Gradle instant execution */
//    val originalArtifactFiles: List<File> = dependency.dependents.flatMap {  it.allModuleArtifacts } .map { it.file }

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
        projectDependency: Project?
    ) : MetadataDependencyResolution(dependency, projectDependency)

    class ExcludeAsUnrequested(
        dependency: ResolvedComponentResult,
        projectDependency: Project?
    ) : MetadataDependencyResolution(dependency, projectDependency)

    abstract class ChooseVisibleSourceSets(
        dependency: ResolvedComponentResult,
        projectDependency: Project?,
        val projectStructureMetadata: KotlinProjectStructureMetadata,
        val allVisibleSourceSetNames: Set<String>,
        val visibleSourceSetNamesExcludingDependsOn: Set<String>,
        val visibleTransitiveDependencies: Set<ResolvedDependencyResult>
    ) : MetadataDependencyResolution(dependency, projectDependency) {
        /** Returns the mapping of source set names to files which should be used as the [dependency] parts representing the source sets.
         * If any temporary files need to be created, their paths are built from the [baseDir].
         * If [doProcessFiles] is true, these temporary files are actually re-created during the call,
         * otherwise only their paths are returned, while the files might be missing.
         */
        fun getMetadataFilesBySourceSet(baseDir: File, doProcessFiles: Boolean): Map<String, FileCollection> =
            getExtractableMetadataFiles(baseDir).getMetadataFilesPerSourceSet(doProcessFiles)

        abstract fun getExtractableMetadataFiles(baseDir: File): ExtractableMetadataFiles

        override fun toString(): String =
            super.toString() + ", sourceSets = " + allVisibleSourceSetNames.joinToString(", ", "[", "]") {
                (if (it in visibleSourceSetNamesExcludingDependsOn) "*" else "") + it
            }
    }
}

internal class GranularMetadataTransformation(
    val project: Project,
    val kotlinSourceSet: KotlinSourceSet,
    /** A list of scopes that the dependencies from [kotlinSourceSet] are treated as requested dependencies. */
    private val sourceSetRequestedScopes: List<KotlinDependencyScope>,
    /** A configuration that holds the dependencies of the appropriate scope for all Kotlin source sets in the project */
    private val parentTransformations: Lazy<Iterable<GranularMetadataTransformation>>
) {
    val metadataDependencyResolutions: Iterable<MetadataDependencyResolution> by lazy { doTransform() }

    // Keep parents of each dependency, too. We need a dependency's parent when it's an MPP's metadata module dependency:
    // in this case, the parent is the MPP's root module.
    private data class ResolvedDependencyWithParent(
        val dependency: ResolvedComponentResult,
        val parent: ResolvedComponentResult?
    )

    private val requestedDependencies: Iterable<Dependency> by lazy {
        requestedDependencies(project, kotlinSourceSet, sourceSetRequestedScopes)
    }

    private val allSourceSetsConfiguration: Configuration =
        commonMetadataDependenciesConfigurationForScopes(project, sourceSetRequestedScopes)

    private val configurationToResolve: Configuration by lazy {
        resolvableMetadataConfiguration(project, allSourceSetsConfiguration, requestedDependencies)
    }

    private fun doTransform(): Iterable<MetadataDependencyResolution> {
        val result = mutableListOf<MetadataDependencyResolution>()

        val parentResolutions =
            parentTransformations.value.flatMap { it.metadataDependencyResolutions }.groupBy {
                ModuleIds.fromComponent(project, it.dependency)
            }

        val allRequestedDependencies = requestedDependencies

        val resolutionResult = configurationToResolve.incoming.resolutionResult
        val allModuleDependencies =
            configurationToResolve.incoming.resolutionResult.allDependencies.filterIsInstance<ResolvedDependencyResult>()

        val resolvedDependencyQueue: Queue<ResolvedDependencyWithParent> = ArrayDeque<ResolvedDependencyWithParent>().apply {
            val requestedModules: Set<ModuleDependencyIdentifier> = allRequestedDependencies.mapTo(mutableSetOf()) {
                ModuleIds.fromDependency(it)
            }

            addAll(
                resolutionResult.root.dependencies
                    .filter { ModuleIds.fromComponentSelector(project, it.requested) in requestedModules }
                    .filterIsInstance<ResolvedDependencyResult>()
                    .map { ResolvedDependencyWithParent(it.selected, null) }
            )
        }

        val visitedDependencies = mutableSetOf<ResolvedComponentResult>()

        while (resolvedDependencyQueue.isNotEmpty()) {
            val (resolvedDependency: ResolvedComponentResult, parent: ResolvedComponentResult?) = resolvedDependencyQueue.poll()

            visitedDependencies.add(resolvedDependency)

            val dependencyResult = processDependency(
                resolvedDependency,
                parentResolutions[ModuleIds.fromComponent(project, resolvedDependency)].orEmpty(),
                parent
            )

            result.add(dependencyResult)

            val transitiveDependenciesToVisit = when (dependencyResult) {
                is MetadataDependencyResolution.KeepOriginalDependency ->
                    resolvedDependency.dependencies.filterIsInstance<ResolvedDependencyResult>()
                is MetadataDependencyResolution.ChooseVisibleSourceSets -> dependencyResult.visibleTransitiveDependencies
                is MetadataDependencyResolution.ExcludeAsUnrequested -> error("a visited dependency is erroneously considered unrequested")
            }

            resolvedDependencyQueue.addAll(
                transitiveDependenciesToVisit.filter { it.selected !in visitedDependencies }
                    .map { ResolvedDependencyWithParent(it.selected, resolvedDependency) }
            )
        }

        allModuleDependencies.forEach { resolvedDependency ->
            if (resolvedDependency.selected !in visitedDependencies) {
//                val files = resolvedDependency.moduleArtifacts.map { it.file }
                result.add(
                    MetadataDependencyResolution.ExcludeAsUnrequested(
                        resolvedDependency.selected,
                        (resolvedDependency.selected.id as? ProjectComponentIdentifier)
                            ?.takeIf { it.build.isCurrentBuild() }
                            ?.let { project.project(it.projectPath) }
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
        module: ResolvedComponentResult,
        parentResolutionsForModule: Iterable<MetadataDependencyResolution>,
        parent: ResolvedComponentResult?
    ): MetadataDependencyResolution {
        val mppDependencyMetadataExtractor = getMetadataExtractor(
            project,
            module,
            configurationToResolve,
            resolveViaAvailableAt = false // we will process the available-at module as a dependency later in the queue
        )

        val resolvedToProject: Project? = module.toProjectOrNull(project)

        val projectStructureMetadata = mppDependencyMetadataExtractor?.getProjectStructureMetadata()
            ?: return MetadataDependencyResolution.KeepOriginalDependency(module, resolvedToProject)

        val sourceSetVisibility =
            SourceSetVisibilityProvider(project).getVisibleSourceSets(
                kotlinSourceSet,
                sourceSetRequestedScopes,
                if (projectStructureMetadata.isPublishedAsRoot) module else parent, module,
                projectStructureMetadata,
                resolvedToProject
            )

        if (mppDependencyMetadataExtractor is JarArtifactMppDependencyMetadataExtractor) {
            mppDependencyMetadataExtractor.metadataArtifactBySourceSet.putAll(sourceSetVisibility.hostSpecificMetadataArtifactBySourceSet)
        }

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
            .filterTo(mutableSetOf()) { ModuleIds.fromComponent(project, it.selected) in requestedTransitiveDependencies }

        val visibleSourceSetsExcludingDependsOn = allVisibleSourceSets.filterTo(mutableSetOf()) { it !in sourceSetsVisibleInParents }

        return ChooseVisibleSourceSetsImpl(
            module, resolvedToProject, projectStructureMetadata, allVisibleSourceSets, visibleSourceSetsExcludingDependsOn,
            transitiveDependenciesToVisit, mppDependencyMetadataExtractor
        )
    }
}

internal class ChooseVisibleSourceSetsImpl(
    dependency: ResolvedComponentResult,
    projectDependency: Project?,
    projectStructureMetadata: KotlinProjectStructureMetadata,
    allVisibleSourceSetNames: Set<String>,
    visibleSourceSetNamesExcludingDependsOn: Set<String>,
    visibleTransitiveDependencies: Set<ResolvedDependencyResult>,
    private val metadataExtractor: MppDependencyMetadataExtractor
) : MetadataDependencyResolution.ChooseVisibleSourceSets(
    dependency,
    projectDependency,
    projectStructureMetadata,
    allVisibleSourceSetNames,
    visibleSourceSetNamesExcludingDependsOn,
    visibleTransitiveDependencies
) {
    override fun getExtractableMetadataFiles(baseDir: File): ExtractableMetadataFiles =
        metadataExtractor.getExtractableMetadataFiles(visibleSourceSetNamesExcludingDependsOn, baseDir)
}

internal fun ResolvedComponentResult.toProjectOrNull(currentProject: Project): Project? {
    val identifier = id
    return when {
        identifier is ProjectComponentIdentifier && identifier.build.isCurrentBuild -> currentProject.project(identifier.projectPath)
        else -> null
    }
}

internal fun resolvableMetadataConfiguration(
    project: Project,
    sourceSets: Iterable<KotlinSourceSet>,
    scopes: Iterable<KotlinDependencyScope>
) = resolvableMetadataConfiguration(
    project,
    commonMetadataDependenciesConfigurationForScopes(project, scopes),
    sourceSets.flatMapTo(mutableListOf()) { requestedDependencies(project, it, scopes) }
)

/** If a source set is not a published source set, its dependencies are not included in [allSourceSetsConfiguration].
 * In that case, to resolve the dependencies of the source set in a way that is consistent with the published source sets,
 * we need to create a new configuration with the dependencies from both [allSourceSetsConfiguration] and the
 * other [requestedDependencies] */
// TODO: optimize by caching the resulting configurations?
internal fun resolvableMetadataConfiguration(
    project: Project,
    allSourceSetsConfiguration: Configuration,
    requestedDependencies: Iterable<Dependency>
): Configuration {
    var modifiedConfiguration: Configuration? = null

    val originalDependencies = allSourceSetsConfiguration.allDependencies

    requestedDependencies.forEach { dependency ->
        if (dependency !in originalDependencies) {
            modifiedConfiguration = modifiedConfiguration ?: project.configurations.detachedConfiguration().apply {
                fun <T> copyAttribute(key: Attribute<T>) {
                    attributes.attribute(key, allSourceSetsConfiguration.attributes.getAttribute(key)!!)
                }
                allSourceSetsConfiguration.attributes.keySet().forEach { copyAttribute(it) }
                allSourceSetsConfiguration.extendsFrom.forEach { extendsFrom(it) }
                dependencies.addAll(originalDependencies) // TODO: check if this line is redundant
            }
            modifiedConfiguration!!.dependencies.add(dependency)
        }
    }
    return modifiedConfiguration ?: allSourceSetsConfiguration
}

/** The configuration that contains the dependencies of the corresponding scopes (and maybe others)
 * from all published source sets. */
internal fun commonMetadataDependenciesConfigurationForScopes(
    project: Project,
    scopes: Iterable<KotlinDependencyScope>
): Configuration {
    // TODO: what if 'runtimeOnly' is combined with 'compileOnly'? prohibit this or merge the two? we never do that now, though
    val configurationName = if (KotlinDependencyScope.RUNTIME_ONLY_SCOPE in scopes)
        ALL_RUNTIME_METADATA_CONFIGURATION_NAME
    else
        ALL_COMPILE_METADATA_CONFIGURATION_NAME
    return project.configurations.getByName(configurationName)
}

internal fun requestedDependencies(
    project: Project,
    sourceSet: KotlinSourceSet,
    requestedScopes: Iterable<KotlinDependencyScope>
): Iterable<Dependency> {
    fun collectScopedDependenciesFromSourceSet(sourceSet: KotlinSourceSet): Set<Dependency> =
        requestedScopes.flatMapTo(mutableSetOf()) { scope ->
            project.sourceSetDependencyConfigurationByScope(sourceSet, scope).incoming.dependencies
        }

    val otherContributingSourceSets = dependsOnClosureWithInterCompilationDependencies(project, sourceSet)
    return listOf(sourceSet, *otherContributingSourceSets.toTypedArray()).flatMap(::collectScopedDependenciesFromSourceSet)
}


internal abstract class MppDependencyMetadataExtractor(val project: Project, val component: ResolvedComponentResult) {
    abstract fun getProjectStructureMetadata(): KotlinProjectStructureMetadata?

    abstract fun getExtractableMetadataFiles(
        visibleSourceSetNames: Set<String>,
        baseDir: File
    ): ExtractableMetadataFiles
}

private class ProjectMppDependencyMetadataExtractor(
    project: Project,
    dependency: ResolvedComponentResult,
    val moduleIdentifier: KotlinModuleIdentifier,
    val dependencyProject: Project
) : MppDependencyMetadataExtractor(project, dependency) {
    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? {
        val topLevelExtension = dependencyProject.topLevelExtension
        return when {
            topLevelExtension is KotlinPm20ProjectExtension -> buildProjectStructureMetadata(
                topLevelExtension.modules.single { it.moduleIdentifier == moduleIdentifier }
            )
            else -> buildKotlinProjectStructureMetadata(dependencyProject)
        }
    }

    override fun getExtractableMetadataFiles(
        visibleSourceSetNames: Set<String>,
        baseDir: File
    ): ExtractableMetadataFiles {
        val result = when (val projectExtension = dependencyProject.topLevelExtension) {
            is KotlinMultiplatformExtension -> projectExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME).compilations
                .filter { it.name in visibleSourceSetNames }.associate { it.defaultSourceSet.name to it.output.classesDirs }
            is KotlinPm20ProjectExtension -> {
                require(moduleIdentifier != null)
                val moduleId = moduleIdentifier
                val module = projectExtension.modules.single { it.moduleIdentifier == moduleId }
                val metadataCompilationRegistry = projectExtension.metadataCompilationRegistryByModuleId.getValue(moduleId)
                visibleSourceSetNames.associateWith {
                    metadataCompilationRegistry.byFragment(module.fragments.getByName(it)).output.classesDirs
                }
            }
            else -> error("unexpected top-level Kotlin extension $projectExtension")
        }

        return object : ExtractableMetadataFiles() {
            override fun getMetadataFilesPerSourceSet(doProcessFiles: Boolean): Map<String, FileCollection> = result
        }
    }
}

private class IncludedBuildMetadataExtractor(
    project: Project,
    dependency: ResolvedComponentResult,
    primaryArtifact: File
) : JarArtifactMppDependencyMetadataExtractor(project, dependency, primaryArtifact) {

    private val id: ProjectComponentIdentifier

    init {
        val id = dependency.id
        require(id is ProjectComponentIdentifier) { "dependency should resolve to a project" }
        require(!id.build.isCurrentBuild) { "should be a project from an included build" }
        this.id = id
    }

    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? =
        GlobalProjectStructureMetadataStorage.getProjectStructureMetadata(project, id.build.name, id.projectPath)
}

internal open class JarArtifactMppDependencyMetadataExtractor(
    project: Project,
    dependency: ResolvedComponentResult,
    val primaryArtifact: File
) : MppDependencyMetadataExtractor(project, dependency) {

    // TODO: add proper API to make an artifact extractor with just the composite artifact "evolve" into one with host-specific artifacts
    val metadataArtifactBySourceSet: MutableMap<String, File> = mutableMapOf()

    private fun parseJsonProjectStructureMetadata(input: InputStream) =
        parseKotlinSourceSetMetadataFromJson(input.reader().readText())

    private fun parseXmlProjectStructureMetadata(input: InputStream) =
        parseKotlinSourceSetMetadataFromXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input))

    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? {
        return ZipFile(primaryArtifact).use { zip ->
            val (metadata, parseFunction) =
                zip.getEntry("META-INF/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME")?.to(::parseJsonProjectStructureMetadata)
                    ?: zip.getEntry("META-INF/$MULTIPLATFORM_PROJECT_METADATA_FILE_NAME")?.to(::parseXmlProjectStructureMetadata)
                    ?: return null

            zip.getInputStream(metadata).use(parseFunction)
        }
    }

    override fun getExtractableMetadataFiles(
        visibleSourceSetNames: Set<String>,
        baseDir: File
    ): ExtractableMetadataFiles {
        val primaryArtifact = primaryArtifact
        val moduleId = ModuleIds.fromComponent(project, component)

        return JarExtractableMetadataFiles(
            moduleId,
            project,
            baseDir,
            visibleSourceSetNames.associate { it to (metadataArtifactBySourceSet[it] ?: primaryArtifact) },
            checkNotNull(getProjectStructureMetadata()) { "project structure metadata is needed to extract files" }
        )
    }

    private class JarExtractableMetadataFiles(
        private val module: ModuleDependencyIdentifier,
        private val project: Project,
        private val baseDir: File,
        private val artifactBySourceSet: Map<String, File>,
        private val projectStructureMetadata: KotlinProjectStructureMetadata
    ) : ExtractableMetadataFiles() {

        override fun getMetadataFilesPerSourceSet(doProcessFiles: Boolean): Map<String, FileCollection> {
            val moduleString = "${module.groupId}-${module.moduleId}"
            val transformedModuleRoot = run { baseDir.resolve(moduleString).also { it.mkdirs() } }

            val resultFiles = mutableMapOf<String, FileCollection>()
            val projectStructureMetadata = projectStructureMetadata

            artifactBySourceSet.forEach { (sourceSetName, artifact) ->
                val extension = projectStructureMetadata.sourceSetBinaryLayout[sourceSetName]?.archiveExtension
                    ?: SourceSetMetadataLayout.METADATA.archiveExtension
                val extractToJarFile = transformedModuleRoot.resolve("$moduleString-$sourceSetName.$extension")

                /** NB: the result may contain files that do not exist and won't be created if the actual metadata artifact doesn't contain
                 * entries for the corresponding source set. It's the consumer's responsibility to filter the result if they need only
                 * existing files! */
                resultFiles[sourceSetName] = project.files(extractToJarFile)

                if (doProcessFiles) {
                    if (extractToJarFile.exists()) {
                        extractToJarFile.delete()
                    }

                    /** In composite builds, we don't really need tro process the file in IDE import, so ignore it if it's missing */
                    // refactor: allow only included builds to provide no artifacts, and allow this only in IDE import
                    if (!artifact.isFile) return@forEach

                    ZipFile(artifact).use { zip ->
                        val entries = zip.entries().asSequence().filter { it.name.startsWith("$sourceSetName/") }.toList()

                        // TODO: once IJ supports non-JAR metadata dependencies, extract to a directory, not a JAR
                        // Also, if both IJ and the CLI compiler can read metadata from a path inside a JAR, then no operations will be needed

                        if (entries.any()) {
                            ZipOutputStream(extractToJarFile.outputStream()).use { resultZipOutput ->
                                for (entry in entries) {
                                    if (entry.isDirectory)
                                        continue

                                    // Drop the source set name from the entry path
                                    val resultEntry = ZipEntry(entry.name.substringAfter("/"))

                                    zip.getInputStream(entry).use { inputStream ->
                                        resultZipOutput.putNextEntry(resultEntry)
                                        inputStream.copyTo(resultZipOutput)
                                        resultZipOutput.closeEntry()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return resultFiles
        }
    }
}

internal fun getMetadataExtractor(
    project: Project,
    resolvedComponentResult: ResolvedComponentResult,
    configuration: Configuration,
    resolveViaAvailableAt: Boolean
): MppDependencyMetadataExtractor? {
    val resolvedMppVariantsProvider = ResolvedMppVariantsProvider.get(project)
    val moduleIdentifier = resolvedComponentResult.toSingleModuleIdentifier() // FIXME this loses information about auxiliary module deps
    // TODO check how this code works with multi-capability resolutions

    return mppDependencyMetadataExtractor(
        resolvedMppVariantsProvider,
        moduleIdentifier,
        configuration,
        resolveViaAvailableAt,
        resolvedComponentResult,
        project
    )
}

internal fun getMetadataExtractor(
    project: Project,
    resolvedComponentResult: ResolvedComponentResult,
    moduleIdentifier: KotlinModuleIdentifier,
    configuration: Configuration
): MppDependencyMetadataExtractor? {
    val resolvedMppVariantsProvider = ResolvedMppVariantsProvider.get(project)
    return mppDependencyMetadataExtractor(
        resolvedMppVariantsProvider,
        moduleIdentifier,
        configuration,
        true,
        resolvedComponentResult,
        project
    )
}

private fun mppDependencyMetadataExtractor(
    resolvedMppVariantsProvider: ResolvedMppVariantsProvider,
    moduleIdentifier: KotlinModuleIdentifier,
    configuration: Configuration,
    resolveViaAvailableAt: Boolean,
    resolvedComponentResult: ResolvedComponentResult,
    project: Project
): MppDependencyMetadataExtractor? {
    var resolvedViaAvailableAt = false

    val metadataArtifact = resolvedMppVariantsProvider.getResolvedArtifactByPlatformModule(
        moduleIdentifier,
        configuration
    ) ?: if (resolveViaAvailableAt) {
        resolvedMppVariantsProvider.getHostSpecificMetadataArtifactByRootModule(
            moduleIdentifier,
            configuration
        )?.also {
            resolvedViaAvailableAt = true
        }
    } else null

    val actualComponent = if (resolvedViaAvailableAt) {
        resolvedComponentResult.dependencies.filterIsInstance<ResolvedDependencyResult>().singleOrNull()?.selected
            ?: resolvedComponentResult
    } else resolvedComponentResult

    val moduleId = actualComponent.id
    return when {
        moduleId is ProjectComponentIdentifier -> when {
            moduleId.build.isCurrentBuild ->
                ProjectMppDependencyMetadataExtractor(project, actualComponent, moduleIdentifier, project.project(moduleId.projectPath))
            metadataArtifact != null ->
                IncludedBuildMetadataExtractor(project, actualComponent, metadataArtifact)
            else -> null
        }
        metadataArtifact != null -> JarArtifactMppDependencyMetadataExtractor(project, actualComponent, metadataArtifact)
        else -> null
    }
}

internal fun getProjectStructureMetadata(
    project: Project,
    module: ResolvedComponentResult,
    configuration: Configuration,
    moduleIdentifier: KotlinModuleIdentifier? = null
): KotlinProjectStructureMetadata? {
    val extractor = if (moduleIdentifier != null)
        getMetadataExtractor(project, module, moduleIdentifier, configuration)
    else
        getMetadataExtractor(project, module, configuration, resolveViaAvailableAt = true)

    return extractor?.getProjectStructureMetadata()
}

// This class is needed to encapsulate how we extract the files and point to them in a way that doesn't capture the Gradle project state
internal abstract class ExtractableMetadataFiles {
    abstract fun getMetadataFilesPerSourceSet(doProcessFiles: Boolean): Map<String, FileCollection>
}

