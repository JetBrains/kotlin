/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.granularMetadata

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil
import org.jetbrains.kotlin.gradle.plugin.mpp.CompositeMetadataJar
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import org.jetbrains.kotlin.gradle.plugin.mpp.toJson
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_RUNTIME_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.utils.ResolvedDependencyGraph
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.File
import javax.inject.Inject

private fun platformsOfSourceSet(sourceSet: KotlinSourceSet, project: Project) = CompilationSourceSetUtil
    .compilationsBySourceSets(project)
    .getValue(sourceSet)
    .map { it.platformType }
    .toSet()

private fun collectProjectData(project: Project): Map<String, ProjectData> = project
    .rootProject
    .childProjects
    .mapKeys { it.key.prefixIfNot(":") }
    .mapValues { ProjectData(it.value) }

private fun compilationDataOfSourceSet(kotlinSourceSet: KotlinSourceSet, project: Project): List<CompilationData> {
    val compilations = CompilationSourceSetUtil.compilationsBySourceSets(project)[kotlinSourceSet] ?: error("Unknown sourceset $kotlinSourceSet")

    return compilations
        .filterNot { it.platformType == KotlinPlatformType.common }
        .map { CompilationData(project, it) }
}

private inline fun <K, T, R: Any> Map<K, T>.mapValuesNotNull(code: (T) -> R?): Map<K, R> =
    mapNotNull { (key, value) -> code(value)?.let { key to it } }.toMap()

data class CompilationData(
    val name: String,
    val platformType: KotlinPlatformType,
    val compileDependencies: ResolvedDependencyGraph
) {
    constructor(project: Project, compilation: KotlinCompilation<*>): this(
        name = compilation.compilationName,
        platformType = compilation.platformType,
        compileDependencies = project
            .configurations
            .getByName(compilation.compileDependencyConfigurationName)
            .let(::ResolvedDependencyGraph)
    )

    override fun toString(): String = "CompilationData of ${platformType.name}/$name"
}

open class TransformKotlinGranularMetadata2
@Inject constructor(
    private val settings: Settings
) : DefaultTask() {

    @get:Classpath
    internal val classpath get() = settings.inputClasspath

    class Settings(
        val kotlinSourceSetName: String,
        //val participatingSourceSetNames: Set<String>,
        val directDependencies: Set<DependencyId>,
        val platforms: Set<KotlinPlatformType>,
        val projects: Map<String, ProjectData>,
        val psmByProjectPath: Map<String, Provider<KotlinProjectStructureMetadata?>>,
        val compilationData: List<CompilationData>,
        val inputClasspath: FileCollection
    ) {
        constructor(kotlinSourceSet: KotlinSourceSet, project: Project): this(
            kotlinSourceSetName = kotlinSourceSet.name,
            //participatingSourceSetNames = kotlinSourceSet.withDependsOnClosure.map { it.name }.toSet(),
            directDependencies = project
                .configurations
                .getByName(kotlinSourceSet.apiConfigurationName)
                .dependencies
                .map { DependencyId(it) }
                .toSet(),
            platforms = platformsOfSourceSet(kotlinSourceSet, project),
            projects = collectProjectData(project),
            psmByProjectPath = project
                .rootProject
                .childProjects
                .mapKeys { it.key.prefixIfNot(":") }
                .mapValuesNotNull { prj -> project.provider { prj.multiplatformExtensionOrNull?.kotlinProjectStructureMetadata } },
            compilationData = compilationDataOfSourceSet(kotlinSourceSet, project),
            inputClasspath = project.files({
                project
                    .configurations
                    .getByName(kotlinSourceSet.apiConfigurationName)
                    .dependencies
                    .filterIsInstance<ProjectDependency>()
                    .mapNotNull { it.dependencyProject.kotlinExtensionOrNull }
                    .flatMap { it.targets.filter { it.platformType == KotlinPlatformType.common } }
                    .flatMap { it.compilations }
                    .map { it.output.classesDirs }
            })
        )
    }

    private val moduleIds = ModuleIds2(settings.projects)

    @get:OutputDirectory
    val outputsDir: File by project.provider {
        project.buildDir.resolve("kotlinSourceSetMetadata2/${settings.kotlinSourceSetName}")
    }

    private val allCompileMetadataDependencies = ResolvedDependencyGraph(
        project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME)
    )

    private val allRuntimeMetadataDependencies = ResolvedDependencyGraph(
        project.configurations.getByName(ALL_RUNTIME_METADATA_CONFIGURATION_NAME)
    )

    private fun ResolvedDependencyGraph.findByDependencyId(requestedDependency: DependencyId): Pair<ResolvedDependencyResult, ResolvedArtifactResult?>? {
        val id = moduleIds.fromDependency(requestedDependency)
        val resolvedDependency = root
            .dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .firstOrNull { dependency -> moduleIds.fromComponent(dependency.selected) == id }
            ?: return null

        val resolvedArtifact = artifacts
            .firstOrNull { artifact -> moduleIds.fromComponentId(artifact.id.componentIdentifier) == id }

        return resolvedDependency to resolvedArtifact
    }

    private val psmExtractor = PSMExtractor(
        psmByProjectPath = settings.psmByProjectPath
    )

    private fun extractPsm(dependencyResult: ResolvedDependencyResult, artifact: ResolvedArtifactResult?): KotlinProjectStructureMetadata? {
        return psmExtractor.extract(dependencyResult, artifact)
    }

    @TaskAction
    fun transformMetadata() {
        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        outputsDir.mkdirs()

        val resolvedDependencies = settings.directDependencies.mapNotNull { dependencyId ->
            allCompileMetadataDependencies.findByDependencyId(dependencyId)
        }
        val queue = ArrayDeque(resolvedDependencies)

        while(queue.isNotEmpty()) {
            val (resolvedDependency, resolvedArtifact) = queue.removeFirst()
            println("Processing: ${resolvedDependency.selected.id}")
            //queue.addAll(resolvedDependency.selected.dependencies.)

            val psm = extractPsm(resolvedDependency, resolvedArtifact)
            println("PSM ${psm?.toJson()}")
            val matchedVariants = getResolvedVariants(resolvedDependency)
            println("Variants: ${matchedVariants.mapValues { it.value?.resolvedVariant }}")

            if (psm == null) continue

            // TODO: Handle null variants. Host-specific sourcests.
            val matchedVariantsNotNull = matchedVariants.values.filterNotNull()
            val visibleSourceSets = getVisibleSourceSets(psm, matchedVariantsNotNull)
            println("Visible sourcesets: $visibleSourceSets")

            if (resolvedArtifact != null) {
                val transformedKlibs = extractMetadataToOutput(
                    resolvedDependency = resolvedDependency,
                    psm = psm,
                    metadataArtifact = resolvedArtifact,
                    visibleSourceSets = visibleSourceSets
                )

                println("Transformed KLIBS: $transformedKlibs")
            } else {
                println("TODO: How to get from Project classpaths")
            }
        }
    }

    private fun getResolvedVariants(resolvedDependency: ResolvedDependencyResult) = settings
        .compilationData
        .associateWith { findPlatformVariant(resolvedDependency, it.compileDependencies) }

    private fun findPlatformVariant(metadataVariant: ResolvedDependencyResult, inGraph: ResolvedDependencyGraph): ResolvedDependencyResult? {
        val metadataVariantModuleId = moduleIds.fromComponent(metadataVariant.selected)
        val allDeps = inGraph.allDependencies.filterIsInstance<ResolvedDependencyResult>()
        val platformVariant = allDeps.find {
            val platformVariantModuleId = moduleIds.fromComponent(it.selected)

            metadataVariantModuleId == platformVariantModuleId
        }

        return platformVariant
    }

    private fun extractMetadataToOutput(
        resolvedDependency: ResolvedDependencyResult,
        psm: KotlinProjectStructureMetadata,
        metadataArtifact: ResolvedArtifactResult,
        visibleSourceSets: Collection<String>
    ): List<File> {
        val metadataJar = CompositeMetadataJar(
            moduleIdentifier = moduleIds.fromComponent(resolvedDependency.selected).toString(),
            projectStructureMetadata = psm,
            primaryArtifactFile = metadataArtifact.file,
            hostSpecificArtifactsBySourceSet = emptyMap()
        )

        return visibleSourceSets.map { sourceSetName ->
            metadataJar.getSourceSetCompiledMetadata(
                sourceSetName = sourceSetName,
                outputDirectory = outputsDir,
                materializeFile = true
            )
        }
    }

    private fun getVisibleSourceSets(psm: KotlinProjectStructureMetadata, variants: List<ResolvedDependencyResult>): Set<String> {
        val variantNames = variants.map { it.resolvedVariant.displayName.removeSuffix("-published") }
        return variantNames
            .map { psm.sourceSetNamesByVariantName[it] ?: error("Unknown variant name: $it") }
            .reduce { acc, value -> acc intersect value }
    }
}
