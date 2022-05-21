/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.gdt

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.*
import org.jetbrains.kotlin.gradle.plugin.sources.dependsOnClosure
import org.jetbrains.kotlin.gradle.plugin.sources.withDependsOnClosure
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import javax.inject.Inject

private val KotlinSourceSet.transformTaskName get() =
    lowerCamelCaseName(
    "unsafeTransform",
    name,
)

fun getSourceSetMetadataClassPath(project: Project, sourceSet: KotlinSourceSet): FileCollection {

    // Depends on closure
    val dependsOnCompilationOutputs =
        sourceSet.withDependsOnClosure.mapNotNull { hierarchySourceSet ->
        val dependencyCompilation = project.getMetadataCompilationForSourceSet(hierarchySourceSet)
        dependencyCompilation?.output?.classesDirs.takeIf { hierarchySourceSet != sourceSet }
    }

    // Transformed klibs
    val taskProvider = project
        .locateOrRegisterTask<TransformKotlinGranularMetadataTask>(
            name = sourceSet.transformTaskName,
            args = listOf(sourceSet.name),
            invokeWhenRegistered = {},
        ) {
            sourceSet.dependsOnClosure.forEach { dependencySourceSet ->
                val dependencyTask = project.locateTask<TransformKotlinGranularMetadataTask>(dependencySourceSet.transformTaskName)
                if (dependencyTask != null) this.dependsOn(dependencyTask)
            }
        }

    val klibs = taskProvider.map {
        project.fileTree(it.klibs).filter { it.isFile && it.name.endsWith(".klib") }
    }

    // non-hmpp but requested dependencies
//    val nonMppComponents = taskProvider.flatMap { task -> task.nonMppLibs }
//        .map { components -> components.map { it.id }.toSet() }
    val nonMppLibs = taskProvider.map { task ->
        val nonMppComponents = task.nonMppLibs.get().map { it.id }.toSet()
        task.artifacts
            .filter { it.id.componentIdentifier in nonMppComponents }
            .map { it.file }
    }

    return project.files(
        listOfNotNull(
            dependsOnCompilationOutputs,
            klibs,
            nonMppLibs
        )
    ).builtBy(taskProvider)
}

fun registerTransformMetadataTasks(project: Project) {
    val tasks = project
        .multiplatformExtension
        .metadata()
        .compilations
        .filter { it.name != "main" } // we have commonMain alread
        .associate { compilation ->
            val sourceSet = compilation.defaultSourceSet
            val taskName = sourceSet.transformTaskName
            sourceSet to project.tasks.register(taskName, TransformKotlinGranularMetadataTask::class.java, sourceSet.name)
        }

    project.multiplatformExtension.sourceSets.forEach { seedSourceSet ->
        val seedTask = tasks[seedSourceSet] ?: return@forEach
        seedSourceSet.dependsOnClosure.forEach x@{ sourceSet ->
            val task = tasks[sourceSet] ?: return@x
            seedTask.dependsOn(task)
        }
    }
}

open class TransformKotlinGranularMetadataTask
@Inject constructor(
    @Input
    val sourceSetName: String,
    providerFactory: ProviderFactory,
) : DefaultTask() {
    @get:OutputDirectory
    val outputsDir: File = project
        .buildDir
        .resolve("kotlinSourceSetMetadata2/${sourceSetName}")

    @get:OutputDirectory
    val klibs = outputsDir.resolve("klibs")

    @Suppress("unused") // Gradle input
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val allSourceSetsMetadataConfiguration: FileCollection =
        project.files(project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME))

    @Transient // must be used only in configuration
    private val kotlinSourceSet: KotlinSourceSet = project.kotlinExtension.sourceSets.getByName(sourceSetName)

    /**
     * Transformation is sensible to changes in sourceSet hierarchy (including sourceSets names)
     */
    @get:Input
    val participatingSourceSets: Set<String> =
        kotlinSourceSet.withDependsOnClosure.map { it.name }.toMutableSet().apply {
            if (any { it == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME })
                add(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
        }

    // TODO: This needs better implementation since CompilationSourceSetUtil::compilationsBySourceSets
    //  isn't in a good shape
    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputCompilationDependencies: Map<String, Set<List<String?>>> = run {
        val participatingCompilations = CompilationSourceSetUtil
            .compilationsBySourceSets(project)
            .filterKeys { it.name in participatingSourceSets }
            .values
            .flatten()

        participatingCompilations.associate {
            lowerCamelCaseName(it.target.name, it.name) to project.configurations.getByName(it.compileDependencyConfigurationName)
                .allDependencies.map { listOf(it.group, it.name, it.version) }.toSet()
        }
    }

    @get:Internal
    internal val sourceSetRequestedScopes: List<KotlinDependencyScope> = listOf(API_SCOPE, IMPLEMENTATION_SCOPE, COMPILE_ONLY_SCOPE)

    @get:Internal
    internal val resolutionResult = project
        .configurations
        .getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME)
        .incoming
        .resolutionResult
        .let { rr -> providerFactory.provider { rr.root } }

    @get:Internal
    internal val artifacts = project
        .configurations
        .getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME)
        .incoming
        .artifactView { view ->
            view.attributes { attrs -> attrs.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA)) }
            view.lenient(true)
        }
        .artifacts

    @get:Internal
    internal val mppLibs = resolutionResult
        .map { rr -> rr.allComponents.filter { it.isMpp } }

    @get:Internal
    internal val nonMppLibs = resolutionResult
        .map { rr -> rr.allComponents.filter { !it.isMpp } }

    private val projectsByPath: Map<String, CacheableProject> = project
        .rootProject
        .allprojects
        .associate { it.path to CacheableProject(it) }

    private val compilationResolutionResults = CompilationSourceSetUtil
        .compilationsBySourceSets(project)
        .getValue(kotlinSourceSet)
        .filter { it.target.platformType != KotlinPlatformType.common }
        .flatMapTo(mutableSetOf()) { compilation ->
            // To find out which variant the MPP dependency got resolved for each compilation, take the resolvable configurations
            // that we have in the compilations:
            sourceSetRequestedScopes.mapNotNull { scope -> project.resolvableConfigurationFromCompilationByScope(compilation, scope) }
        }
        .associate { it.name to it.incoming.resolutionResult.let { rr -> providerFactory.provider { rr.root } } }


    private fun Project.resolvableConfigurationFromCompilationByScope(
        compilation: KotlinCompilation<*>,
        scope: KotlinDependencyScope
    ): Configuration? {
        val configurationName = when (scope) {
            API_SCOPE, IMPLEMENTATION_SCOPE, COMPILE_ONLY_SCOPE -> compilation.compileDependencyConfigurationName
            RUNTIME_ONLY_SCOPE ->
                (compilation as? KotlinCompilationToRunnableFiles<*>)?.runtimeDependencyConfigurationName
                    ?: return null
        }

        return project.configurations.getByName(configurationName)
    }

//    @get:Internal
//    val dependsOnCompilationOutputs =
//        kotlinSourceSet.withDependsOnClosure.mapNotNull { hierarchySourceSet ->
//        val dependencyCompilation = project.getMetadataCompilationForSourceSet(hierarchySourceSet)
//        dependencyCompilation?.output?.classesDirs.takeIf { hierarchySourceSet != kotlinSourceSet }
//    }

    private val moduleIds = ModuleIds(projectsByPath)

    fun loadPsmFromOutput(moduleId: ModuleDependencyIdentifier): KotlinProjectStructureMetadata {
        return outputsDir
            .resolve("psm")
            .resolve("${moduleId}.json")
            .readText()
            .let { parseKotlinSourceSetMetadataFromJson(it) }
            ?: error("Failed to parse PSM for $moduleId")
    }

    @TaskAction
    fun transformMetadata() {
        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        outputsDir.mkdirs()

        val psmMap = PersistingPSMExtractor(moduleIds).extract(
            resolutionResult.get(),
            artifacts,
            outputsDir.resolve("psm").also { it.mkdirs() }
        )

        val platformComponents = compilationResolutionResults
            .mapValues { (_, provider) -> provider.get() }

        val visibleSourceSets = resolutionResult
            .get()
            .allComponents
            .filter { it.isMpp }
            .associateWith { metadataComponent ->
                val matchedPlatformVariants = platformComponents.mapValues { (_, root) ->
                    findMatchedPlatformVariant(metadataComponent, root)
                        ?: error("Can't match variant $metadataComponent with $root")
                }
                val psm = psmMap[metadataComponent] ?: error("Psm not found for: $metadataComponent")

                matchedPlatformVariants.map { (name, variant) ->
                    val variantName = variant.displayName.removeSuffix("-published")
                    psm.second.sourceSetNamesByVariantName[variantName] ?: error("No variant found in PSM: $variantName")
                }.reduce { visible, sourceSets -> visible.intersect(sourceSets) }
            }

        //val klibFiles = mutableSetOf<File>()
        for ((component, sourceSets) in visibleSourceSets) {
            println("$component => $sourceSets")
            val psm = psmMap[component] ?: error("Psm not found for: $component")
            val jar = CompositeMetadataJar(
                moduleIdentifier = moduleIds.fromComponent(component).toString(),
                projectStructureMetadata = psm.second,
                primaryArtifactFile = psm.first,
                hostSpecificArtifactsBySourceSet = emptyMap()
            )
            for (sourceSet in sourceSets) {
                jar.getSourceSetCompiledMetadata(sourceSet, klibs, true)
            }
        }
    }

    private val ResolvedComponentResult.isMpp get() =
        dependents.isNotEmpty() && // filter out the root of the dependency graph, we are not interested in it
                variants.any { variant -> variant.attributes.keySet().any { it.name == KotlinPlatformType.attribute.name } }
}

internal fun findMatchedPlatformVariant(
    metadata: ResolvedComponentResult,
    rootPlatform: ResolvedComponentResult
) = rootPlatform
    .allComponents
    .find { metadata.id == it.id }
    ?.variants
    ?.first()

