/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

internal sealed class MetadataDependencyResolution(
    val dependency: ResolvedComponentResult,
    val projectDependency: Project?
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
        abstract fun getMetadataFilesBySourceSet(baseDir: File, doProcessFiles: Boolean): Map<String, FileCollection>

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
    private val allSourceSetsConfiguration: Configuration,
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
        fun collectScopedDependenciesFromSourceSet(sourceSet: KotlinSourceSet): Set<Dependency> =
            sourceSetRequestedScopes.flatMapTo(mutableSetOf()) { scope ->
                project.sourceSetDependencyConfigurationByScope(sourceSet, scope).incoming.dependencies
            }

        val ownDependencies = collectScopedDependenciesFromSourceSet(kotlinSourceSet)
        val parentDependencies = parentTransformations.value.flatMapTo(mutableSetOf<Dependency>()) { it.requestedDependencies }

        ownDependencies + parentDependencies
    }

    private val configurationToResolve: Configuration by lazy {
        /** If [kotlinSourceSet] is not a published source set, its dependencies are not included in [allSourceSetsConfiguration].
         * In that case, to resolve the dependencies of the source set in a way that is consistent with the published source sets,
         * we need to create a new configuration with the dependencies from both [allSourceSetsConfiguration] and the
         * input configuration(s) of the source set. */
        var modifiedConfiguration: Configuration? = null

        val originalDependencies = allSourceSetsConfiguration.allDependencies

        requestedDependencies.forEach { dependency ->
            if (dependency !in originalDependencies) {
                modifiedConfiguration = (modifiedConfiguration ?: allSourceSetsConfiguration.copyRecursive()).apply {
                    dependencies.add(dependency)
                }
            }
        }

        modifiedConfiguration ?: allSourceSetsConfiguration
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

            val resolvedToProject: Project? = (resolvedDependency.id as? ProjectComponentIdentifier)?.projectPath?.let(project::project)

            visitedDependencies.add(resolvedDependency)

            val dependencyResult = processDependency(
                resolvedDependency,
                parentResolutions[ModuleIds.fromComponent(project, resolvedDependency)].orEmpty(),
                parent,
                resolvedToProject
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
                result.add(
                    MetadataDependencyResolution.ExcludeAsUnrequested(
                        resolvedDependency.selected,
                        (resolvedDependency.selected.id as? ProjectComponentIdentifier)?.let { project.project(it.projectPath) }
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
        parent: ResolvedComponentResult?,
        resolvedToProject: Project?
    ): MetadataDependencyResolution {
        val resolvedMppVariantsProvider = ResolvedMppVariantsProvider.get(project)

        val mppDependencyMetadataExtractor = if (resolvedToProject != null) {
            ProjectMppDependencyMetadataExtractor(project, module, resolvedToProject)
        } else {
            val metadataArtifact = resolvedMppVariantsProvider.getPlatformArtifactByPlatformModule(
                ModuleIds.fromComponent(project, module),
                configurationToResolve
            )
            if (metadataArtifact != null) {
                JarArtifactMppDependencyMetadataExtractor(project, module, metadataArtifact)
            } else null
        }

        val projectStructureMetadata = mppDependencyMetadataExtractor?.getProjectStructureMetadata()
            ?: return MetadataDependencyResolution.KeepOriginalDependency(module, resolvedToProject)

        val sourceSetVisibility =
            SourceSetVisibilityProvider(project).getVisibleSourceSets(
                kotlinSourceSet,
                sourceSetRequestedScopes,
                parent, module,
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

    private class ChooseVisibleSourceSetsImpl(
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
        override fun getMetadataFilesBySourceSet(baseDir: File, doProcessFiles: Boolean): Map<String, FileCollection> =
            metadataExtractor.getVisibleSourceSetsMetadata(visibleSourceSetNamesExcludingDependsOn, baseDir, doProcessFiles)
    }
}

private abstract class MppDependencyMetadataExtractor(val project: Project, val dependency: ResolvedComponentResult) {
    abstract fun getProjectStructureMetadata(): KotlinProjectStructureMetadata?
    abstract fun getVisibleSourceSetsMetadata(
        visibleSourceSetNames: Set<String>,
        baseDir: File,
        doProcessFiles: Boolean
    ): Map<String, FileCollection>
}

private class ProjectMppDependencyMetadataExtractor(
    project: Project,
    dependency: ResolvedComponentResult,
    private val dependencyProject: Project
) : MppDependencyMetadataExtractor(project, dependency) {
    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? =
        buildKotlinProjectStructureMetadata(dependencyProject)

    override fun getVisibleSourceSetsMetadata(
        visibleSourceSetNames: Set<String>,
        baseDir: File,
        doProcessFiles: Boolean
    ): Map<String, FileCollection> =
        dependencyProject.multiplatformExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME).compilations
            .filter { it.name in visibleSourceSetNames }
            .associate { it.defaultSourceSet.name to it.output.classesDirs }
}

private class JarArtifactMppDependencyMetadataExtractor(
    project: Project,
    dependency: ResolvedComponentResult,
    val primaryArtifact: File
) : MppDependencyMetadataExtractor(project, dependency) {

    val metadataArtifactBySourceSet: MutableMap<String, File> = mutableMapOf()

    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? {
        return ZipFile(primaryArtifact).use { zip ->
            val metadata = zip.getEntry("META-INF/$MULTIPLATFORM_PROJECT_METADATA_FILE_NAME")
                ?: return null

            val metadataXmlDocument = zip.getInputStream(metadata).use { inputStream ->
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
            }

            parseKotlinSourceSetMetadataFromXml(metadataXmlDocument)
        }
    }

    override fun getVisibleSourceSetsMetadata(
        visibleSourceSetNames: Set<String>,
        baseDir: File,
        doProcessFiles: Boolean
    ): Map<String, FileCollection> {
        val primaryArtifact = primaryArtifact
        val moduleId = ModuleIds.fromComponent(project, dependency)

        return extractSourceSetMetadataFromArtifacts(
            moduleId,
            baseDir,
            doProcessFiles,
            visibleSourceSetNames.associate { it to (metadataArtifactBySourceSet[it] ?: primaryArtifact) }
        )
    }

    private fun extractSourceSetMetadataFromArtifacts(
        module: ModuleDependencyIdentifier,
        baseDir: File,
        doProcessFiles: Boolean,
        artifactBySourceSet: Map<String, File>
    ): Map<String, FileCollection> {
        val moduleString = "${module.groupId}-${module.moduleId}"
        val transformedModuleRoot = run { baseDir.resolve(moduleString).also { it.mkdirs() } }

        val resultFiles = mutableMapOf<String, FileCollection>()

        val projectStructureMetadata = checkNotNull(getProjectStructureMetadata()) {
            "can't extract metadata from a module without project structure metadata"
        }

        artifactBySourceSet.forEach { (sourceSetName, artifact) ->
            ZipFile(artifact).use { zip ->
                val entries = zip.entries().asSequence().filter { it.name.startsWith("$sourceSetName/") }.toList()

                // TODO: once IJ supports non-JAR metadata dependencies, extract to a directory, not a JAR
                // Also, if both IJ and the CLI compiler can read metadata from a path inside a JAR, then no operations will be needed

                if (entries.any()) {
                    val extension = projectStructureMetadata.sourceSetBinaryLayout[sourceSetName]?.archiveExtension
                        ?: SourceSetMetadataLayout.METADATA.archiveExtension

                    val extractToJarFile = transformedModuleRoot.resolve("$moduleString-$sourceSetName.$extension")
                    resultFiles[sourceSetName] = project.files(extractToJarFile)

                    if (doProcessFiles) {
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
