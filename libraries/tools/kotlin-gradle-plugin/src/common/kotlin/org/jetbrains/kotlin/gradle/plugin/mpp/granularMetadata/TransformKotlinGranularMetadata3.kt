/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.granularMetadata

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil
import org.jetbrains.kotlin.gradle.plugin.mpp.CompositeMetadataJar
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.ResolvedDependencyGraph
import org.jetbrains.kotlin.gradle.utils.allResolvedDependencies
import org.jetbrains.kotlin.gradle.utils.maybeCreate
import org.jetbrains.kotlin.gradle.utils.withType
import java.io.File
import javax.inject.Inject

object KotlinGranularMetadataClasspaths {
    fun classpathFile(projectBuildDir: File, sourceSetName: String): File =
        projectBuildDir
            .resolve("kotlinSourceSetMetadata3")
            .resolve("${sourceSetName}.classpath")

    fun readClasspath(projectBuildDir: File, sourceSetName: String): List<File> {
        return classpathFile(projectBuildDir, sourceSetName).readLines().map(::File)
    }

    fun writeClasspath(projectBuildDir: File, sourceSetName: String, classpath: List<File>) {
        val classpathContent = classpath.joinToString("\n") { it.toRelativeString(projectBuildDir) }
        classpathFile(projectBuildDir, sourceSetName).writeText(classpathContent)
    }
}

abstract class TransformKotlinGranularMetadata3
@Inject constructor(
    private val settings: Settings,
    private val projectLayout: ProjectLayout
) : DefaultTask() {
    class Settings(
        val fragmentName: String,
        val resolvedFragmentDependencies: ResolvedDependencyGraph,
        val resolvedVariantDependencies: Map<String, ResolvedDependencyGraph>,
        val projectsMetadataOutputs: Map<String, Provider<Map<String, FileCollection>>>,
        val psmByProjectPath: Map<String, Provider<KotlinProjectStructureMetadata?>>,
        val projectModuleIds: Map<String, Provider<ModuleDependencyIdentifier>>,
        val resolvedHostSpecificDependencies: List<Provider<ResolvedDependencyGraph>>?
    )

    private val moduleDependencyIdToDependency: Map<ModuleDependencyIdentifier, ResolvedDependencyResult> by lazy {
        settings
            .resolvedFragmentDependencies
            .allResolvedDependencies
            .associateBy { it.selected.id.toModuleDependencyIdentifier() }
    }

    private val projectPath: String = project.path

    private val psmExtractor = PSMExtractor(
        psmByProjectPath = settings.psmByProjectPath
    )

    @get:OutputDirectory
    val outputsDir: File get() = projectLayout
        .buildDirectory
        .get()
        .asFile
        .resolve("kotlinSourceSetMetadata3/${settings.fragmentName}")

    @get:OutputFile
    val classpathFile: File get() = KotlinGranularMetadataClasspaths.classpathFile(
        projectBuildDir = projectLayout.buildDirectory.get().asFile,
        sourceSetName = settings.fragmentName
    )

    @get:Internal
    val transformedClasspath: List<File> get() = classpathFile
        .takeIf { it.exists() }
        ?.readLines()
        .orEmpty()
        .map { File(projectLayout.buildDirectory.asFile.get(), it).normalize() }

    @TaskAction
    fun transform() {
        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        classpathFile.delete()
        outputsDir.mkdirs()

        val allExtractedKlibs = mutableListOf<File>()

        val directDependencies = settings.resolvedFragmentDependencies.root.dependencies.filterIsInstance<ResolvedDependencyResult>()

        val queue = ArrayDeque<ResolvedDependencyResult>(directDependencies)
        val visited = mutableSetOf<ResolvedDependencyResult>()

        while(queue.isNotEmpty()) {
            val dependency = queue.removeFirst()
            visited.add(dependency)

            if (!dependency.isMpp) {
                //allExtractedKlibs += settings.resolvedFragmentDependencies.dependencyArtifacts(dependency).map { it.file }
                continue
            }

            val klibArtifact = settings.resolvedFragmentDependencies.dependencyArtifacts(dependency).single()
            val variants = findVariantsOf(dependency)
            val psm = psmFrom(klibArtifact)
            if (psm == null) {
                allExtractedKlibs += settings.resolvedFragmentDependencies.dependencyArtifacts(dependency).map { it.file }
                continue
            }

            val visibleSourceSets = inferVisibleSourceSets(psm, variants)
            val extractedKlibs = extractSourceSetsMetadata(dependency, klibArtifact, psm, visibleSourceSets)
            allExtractedKlibs += extractedKlibs

            queue.addAll(psm.dependenciesOfSourceSets(visibleSourceSets).filterNot { it in visited })
        }

        KotlinGranularMetadataClasspaths.writeClasspath(projectLayout.buildDirectory.asFile.get(), settings.fragmentName, allExtractedKlibs)
    }

    private fun ComponentIdentifier.toModuleDependencyIdentifier(): ModuleDependencyIdentifier {
        return when (this) {
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(group, module)
            is ProjectComponentIdentifier -> settings.projectModuleIds[projectPath]?.get() ?: error("Cant find project Module ID")
            else -> error("Unknown ComponentIdentifier: $this")
        }
    }

    private fun KotlinProjectStructureMetadata.dependenciesOfSourceSets(sourceSets: Iterable<String>): List<ResolvedDependencyResult> {
        return sourceSets.flatMap { sourceSetModuleDependencies[it] ?: emptySet() }.mapNotNull { moduleDependencyIdToDependency[it] }
    }

    private fun hostSpecificMetadataArtifacts(componentId: ComponentIdentifier, psm: KotlinProjectStructureMetadata): Map<String, File> {
        if (psm.hostSpecificSourceSets.isEmpty()) return emptyMap()

        val graph = settings.resolvedHostSpecificDependencies?.first()?.get() ?: return emptyMap()
        val dependency = graph.allResolvedDependencies.find { it.selected.id == componentId } ?: error("Dependency by $componentId not found")

        val artifacts = graph.dependencyArtifactsOrNull(dependency) ?: return emptyMap()
        val artifactFile = artifacts.singleOrNull()?.file ?: return emptyMap()

        return psm.hostSpecificSourceSets.associateWith { artifactFile }
    }

    private fun ResolvedDependencyResult.requestedModuleId(): ModuleIdentifier? {
        val requestedComponent = (requested as? ModuleComponentSelector) ?: return null
        return requestedComponent.moduleIdentifier
    }

    private fun findVariantsOf(dependency: ResolvedDependencyResult): Set<String> {
        val id = dependency.selected.id
        val variants = settings
            .resolvedVariantDependencies
            .mapNotNull { (_, dependencies) -> dependencies.allResolvedDependencies.find { it.selected.id == id } }
            .map { it.resolvedVariant.displayName.let(::kotlinVariantNameFromPublishedVariantName) }

        return variants.toSet()
    }

    private fun psmFrom(artifact: ResolvedArtifactResult) = psmExtractor.extract(artifact)

    private fun inferVisibleSourceSets(psm: KotlinProjectStructureMetadata, variants: Set<String>): Set<String> {
        return variants
            .map { psm.sourceSetNamesByVariantName[it]!! }
            .takeIf { it.isNotEmpty() }
            ?.reduce { visibleSourceSets, platformSourceSets -> visibleSourceSets intersect platformSourceSets }
            ?: emptySet()
    }

    private fun extractSourceSetsMetadata(
        dependency: ResolvedDependencyResult,
        artifact: ResolvedArtifactResult,
        psm: KotlinProjectStructureMetadata,
        sourceSets: Set<String>
    ): List<File> {
        val id = artifact.variant.owner
        return when {
            id is ProjectComponentIdentifier && id.build.isCurrentBuild -> {
                val projectPath = id.projectPath
                val allProjectMetadata = settings.projectsMetadataOutputs[projectPath]?.get()
                    ?: error("Can't find project metadata $projectPath")
                sourceSets.flatMap { allProjectMetadata[it]?.files ?: error("Can't get metadata for sourceset $it from $projectPath") }
            }
            id is ProjectComponentIdentifier && !id.build.isCurrentBuild -> {
                val moduleId = dependency.requestedModuleId() ?: error("Unknown requested module ID $dependency")
                extractSourceSetsMetadataFromJar(
                    id = moduleId,
                    dependency = dependency,
                    psm = psm,
                    jar = artifact.file,
                    sourceSets = sourceSets
                )
            }
            id is ModuleComponentIdentifier -> extractSourceSetsMetadataFromJar(
                id = id.moduleIdentifier,
                dependency = dependency,
                psm = psm,
                jar = artifact.file,
                sourceSets = sourceSets
            )
            else -> error("unknown module component identifier")
        }
    }

    private fun extractSourceSetsMetadataFromJar(
        id: ModuleIdentifier,
        dependency: ResolvedDependencyResult,
        psm: KotlinProjectStructureMetadata,
        jar: File,
        sourceSets: Set<String>
    ): List<File> {
        val hostSpecificMetadataArtifactBySourceSet = hostSpecificMetadataArtifacts(dependency.selected.id, psm)
        val metadataJar = CompositeMetadataJar(
            moduleIdentifier = "${id.group}-${id.name}",
            projectStructureMetadata = psm,
            primaryArtifactFile = jar,
            hostSpecificArtifactsBySourceSet = hostSpecificMetadataArtifactBySourceSet
        )

        return sourceSets.map { sourceSet ->
            metadataJar.getSourceSetCompiledMetadata(sourceSet, outputsDir, materializeFile = true)
        }
    }

    private val ResolvedDependencyResult.isMpp
        get(): Boolean {
            val attributes = selected
                .variants
                .firstOrNull()
                ?.attributes
                ?: return false

            return attributes.keySet().any { it.name == KotlinPlatformType.attribute.name }
        }
}

class TransformKotlinGranularMetadata3TCSRegistrator(
    private val project: Project
) {
    //private val metadataService = GranularMetadataTransformationService.registerIfAbsent(project)

    private val hostSpecificSourceSets by lazy { getHostSpecificSourceSets(project) }
    private val compilationsBySourceSets by lazy { CompilationSourceSetUtil.compilationsBySourceSets(project) }

    fun configurationName(sourceSet: KotlinSourceSet) = sourceSet.name + "TransformKotlinGranularMetadata3"

    fun registerConfigurations(target: KotlinMetadataTarget, sourceSet: KotlinSourceSet): Configuration {
        val name = configurationName(sourceSet)
        val configurationFound = project.configurations.findByName(name)
        if (configurationFound != null) return configurationFound

        val configurations = listOf(
            sourceSet.apiConfigurationName,
            sourceSet.compileOnlyConfigurationName,
            sourceSet.implementationConfigurationName
        ).map { project.configurations.getByName(it) }.toTypedArray()

        val dependsOnConfigurations = sourceSet.dependsOn.map { registerConfigurations(target, it) }.toTypedArray()

        return project.configurations.create(name) { configuration ->
            configuration.isCanBeResolved = true
            configuration.isCanBeConsumed = false

            configuration.description = "Collection of Compile Metadata created by ${this.javaClass}"

            configuration.usesPlatformOf(target)
            configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
            configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))

            configuration.extendsFrom(*configurations)
            configuration.extendsFrom(*dependsOnConfigurations)
        }
    }

    fun registerForSourceSet(kotlinSourceSet: KotlinSourceSet): TaskProvider<TransformKotlinGranularMetadata3> {
        val compilations = kotlinSourceSet
            .internal
            .compilations
            .filter { it !is KotlinCommonCompilation }
            .filter { it.isMain() }

        val fragmentDependenciesConfiguration = project.configurations.getByName(configurationName(kotlinSourceSet))
        val settings = TransformKotlinGranularMetadata3.Settings(
            fragmentName = kotlinSourceSet.name,
            resolvedFragmentDependencies = ResolvedDependencyGraph(fragmentDependenciesConfiguration),
            resolvedVariantDependencies = compilations.associate {
                (it.target.name + it.name) to project.configurations.getByName(it.compileDependencyConfigurationName)
                    .let(::ResolvedDependencyGraph)
            },
            projectsMetadataOutputs = projectsMetadataOutputs,
            psmByProjectPath = psmByProjectPath,
            projectModuleIds = projectModuleIds(),
            resolvedHostSpecificDependencies = resolvedHostSpecificDependencies(compilations)
        )

        return project.locateOrRegisterTask<TransformKotlinGranularMetadata3>(
            name = "transformKotlinGranularMetadata3" + kotlinSourceSet.name,
            args = listOf(settings)
        ) {
            inputs.files(fragmentDependenciesConfiguration)
        }
    }

    private fun projectModuleIds(): Map<String, Provider<ModuleDependencyIdentifier>> = project
        .allProjects()
        .mapValues { project.provider { ModuleDependencyIdentifier(it.value.group.toString(), it.value.name) } }

    private fun resolvedHostSpecificDependencies(compilations: List<KotlinCompilation<*>>): List<Provider<ResolvedDependencyGraph>>? {
        val isSharedNative = compilations.isNotEmpty() && compilations.all {
            it.platformType == KotlinPlatformType.common || it.platformType == KotlinPlatformType.native
        }

        if (!isSharedNative) return null

        return compilations.map { compilation ->
            compilation as AbstractKotlinNativeCompilation

            val platformArtifactConfiguration = project.configurations.getByName(compilation.compileDependencyConfigurationName)
            val metadataHostSpecific = project.configurations.maybeCreate("hostSpecificMetadataOf" + compilation.compileDependencyConfigurationName) {
                isCanBeResolved = true
                isCanBeConsumed = false

                description = "Host specific metadata of compilation: $compilation"

                extendsFrom(*platformArtifactConfiguration.extendsFrom.toTypedArray())
                copyAttributes(platformArtifactConfiguration.attributes, attributes)
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
            }

            project.provider { ResolvedDependencyGraph(metadataHostSpecific) }
        }
    }

    private val projectsMetadataOutputs by lazy { extractProjectsMetadataOutputs() }

    private fun extractProjectsMetadataOutputs(): Map<String, Provider<Map<String, FileCollection>>> {
        return project
            .allProjects()
            .mapValues { (_, subProject) -> project.provider { subProject.multiplatformExtensionOrNull?.sourceSetsMetadataOutputs() } }
    }

    private fun KotlinMultiplatformExtension.sourceSetsMetadataOutputs(): Map<String, FileCollection> {
        val commonTarget = targets.withType<KotlinMetadataTarget>().singleOrNull() ?: return emptyMap()

        val compilations = commonTarget.compilations

        return sourceSets.mapNotNull { sourceSet ->
            val compilation = compilations.findByName(sourceSet.name)
                ?: return@mapNotNull null // given source set is not shared

            val destination = when (compilation) {
                is KotlinCommonCompilation -> compilation.output.classesDirs
                is KotlinSharedNativeCompilation -> compilation.output.classesDirs
                else -> error("Unexpected compilation type: $compilation")
            }

            Pair(sourceSet.name, destination)
        }.toMap()
    }

    private val psmByProjectPath by lazy { collectProjectPSMs() }

    private fun collectProjectPSMs(): Map<String, Provider<KotlinProjectStructureMetadata?>> {
        return project
            .allProjects()
            .mapValuesNotNull { prj -> project.provider { prj.multiplatformExtensionOrNull?.kotlinProjectStructureMetadata } }
    }

    private fun Project.allProjects(): Map<String, Project> {
        fun Project.allChildProjects(parentPath: String): Map<String, Project> {
            val result = childProjects.mapKeys { "${parentPath}:${it.key}" }
            return result + result.map { it.value.allChildProjects(it.key) }.fold(emptyMap()) { acc, item -> acc + item }
        }
        return mapOf(":" to rootProject) + rootProject.allChildProjects("")
    }

    private inline fun <K, T, R: Any> Map<K, T>.mapValuesNotNull(code: (T) -> R?): Map<K, R> =
        mapNotNull { (key, value) -> code(value)?.let { key to it } }.toMap()
}
