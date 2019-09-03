/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.metadata

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.setArchiveAppendixCompatible
import org.jetbrains.kotlin.gradle.utils.setArchiveClassifierCompatible
import java.io.File
import java.util.concurrent.Callable

internal const val ALL_COMPILE_METADATA_CONFIGURATION_NAME = "allSourceSetsCompileDependenciesMetadata"
internal const val ALL_RUNTIME_METADATA_CONFIGURATION_NAME = "allSourceSetsRuntimeDependenciesMetadata"

internal val Project.isKotlinGranularMetadataEnabled: Boolean
    get() = PropertiesProvider(rootProject).enableGranularSourceSetsMetadata == true

class KotlinMetadataTargetConfigurator(kotlinPluginVersion: String) :
    KotlinTargetConfigurator<KotlinCommonCompilation>(
        createDefaultSourceSets = false,
        createTestCompilation = false,
        kotlinPluginVersion = kotlinPluginVersion
    ) {

    companion object {
        internal const val ALL_METADATA_JAR_NAME = "allMetadataJar"

        internal fun transformGranularMetadataTaskName(sourceSet: KotlinSourceSet) =
            lowerCamelCaseName("transform", sourceSet.name, "DependenciesMetadata")

        // TODO generalize once a general production-test and other kinds of inter-compilation visibility are supported
        // Currently, this is a temporary ad-hoc mechanism for exposing the commonMain dependencies to the test source sets
        internal fun dependsOnWithInterCompilationDependencies(project: Project, sourceSet: KotlinSourceSet): Set<KotlinSourceSet> =
            sourceSet.dependsOn.toMutableSet().apply {
                if (sourceSet.name == KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)
                    add(project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
            }
    }

    private val KotlinOnlyTarget<KotlinCommonCompilation>.apiElementsConfiguration: Configuration
        get() = project.configurations.getByName(apiElementsConfigurationName)

    override fun configureTarget(target: KotlinOnlyTarget<KotlinCommonCompilation>) {
        super.configureTarget(target)

        if (target.project.isKotlinGranularMetadataEnabled) {
            target as KotlinMetadataTarget

            createMergedAllSourceSetsConfigurations(target)

            val allMetadataJar = target.project.tasks.getByName(ALL_METADATA_JAR_NAME) as Jar
            createMetadataCompilationsForCommonSourceSets(target, allMetadataJar)

            setupDependencyTransformationForCommonSourceSets(target)

            target.project.configurations.getByName(target.apiElementsConfigurationName).attributes
                .attribute(Usage.USAGE_ATTRIBUTE, target.project.usageByName(KotlinUsages.KOTLIN_METADATA))
        }
    }

    override fun setupCompilationDependencyFiles(project: Project, compilation: KotlinCompilation<KotlinCommonOptions>) {
        /** See [createTransformedMetadataClasspath] and its usage. */
        if (project.isKotlinGranularMetadataEnabled)
            compilation.compileDependencyFiles = project.files()
        else
            super.setupCompilationDependencyFiles(project, compilation)
    }

    override fun buildCompilationProcessor(compilation: KotlinCommonCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return KotlinCommonSourceSetProcessor(compilation.target.project, compilation, tasksProvider, kotlinPluginVersion)
    }

    override fun createJarTasks(target: KotlinOnlyTarget<KotlinCommonCompilation>) {
        super.createJarTasks(target)

        if (target.project.isKotlinGranularMetadataEnabled) {
            /** This JAR is created in addition to the main one, published with a classifier, but is by default used
             * for project dependencies (as the Kotlin Granular metadata is enabled across all projects in a build, this is OK).
             * See also [KotlinMetadataTarget.kotlinComponents]
             */
            target.project.tasks.create(ALL_METADATA_JAR_NAME, Jar::class.java).apply {
                description = "Assembles a jar archive containing the metadata for all Kotlin source sets."
                group = BasePlugin.BUILD_GROUP

                setArchiveAppendixCompatible { target.name.toLowerCase() }
                setArchiveClassifierCompatible { "all" }
            }
        }
    }

    private fun setupDependencyTransformationForCommonSourceSets(target: KotlinMetadataTarget) {
        target.project.whenEvaluated {
            val publishedCommonSourceSets: Set<KotlinSourceSet> = getPublishedCommonSourceSets(project)

            kotlinExtension.sourceSets.all {
                setupDependencyTransformationForSourceSet(target.project, it, it in publishedCommonSourceSets)
            }
        }
    }

    private fun createMetadataCompilationsForCommonSourceSets(
        target: KotlinMetadataTarget,
        allMetadataJar: Jar
    ) = target.project.whenEvaluated {
        // Do this after all targets are configured by the user build script

        val publishedCommonSourceSets: Set<KotlinSourceSet> = getPublishedCommonSourceSets(project)

        val sourceSetsWithMetadataCompilations: Map<KotlinSourceSet, KotlinCommonCompilation> =
            publishedCommonSourceSets.associate { sourceSet ->
                sourceSet to configureMetadataCompilation(target, sourceSet, allMetadataJar)
            }

        sourceSetsWithMetadataCompilations.forEach { (sourceSet, metadataCompilation) ->
            val compileMetadataTransformationTasksForHierarchy = mutableSetOf<TaskHolder<TransformKotlinGranularMetadata>>()

            // Adjust metadata compilation to support source set hierarchies, i.e. use both the outputs of dependsOn source set compilation
            // and their dependencies metadata transformed for compilation:
            sourceSet.getSourceSetHierarchy().forEach { hierarchySourceSet ->
                if (hierarchySourceSet != sourceSet) {
                    val dependencyCompilation = sourceSetsWithMetadataCompilations.getValue(hierarchySourceSet as DefaultKotlinSourceSet)
                    metadataCompilation.compileDependencyFiles += dependencyCompilation.output.classesDirs.filter { it.exists() }
                }

                project.locateTask<TransformKotlinGranularMetadata>(transformGranularMetadataTaskName(hierarchySourceSet))
                    ?.let(compileMetadataTransformationTasksForHierarchy::add)
            }

            metadataCompilation.compileDependencyFiles += createTransformedMetadataClasspath(
                project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME),
                project,
                compileMetadataTransformationTasksForHierarchy
            )
        }

        val generateMetadata = createGenerateProjectStructureMetadataTask()

        allMetadataJar.from(project.files(Callable {
            generateMetadata.doGetTask().resultXmlFile
        }).builtBy(generateMetadata.getTaskOrProvider())) { spec ->
            spec.into("META-INF").rename { MULTIPLATFORM_PROJECT_METADATA_FILE_NAME }
        }
    }

    private fun createMergedAllSourceSetsConfigurations(target: KotlinMetadataTarget): Unit = with(target.project) {
        listOf(ALL_COMPILE_METADATA_CONFIGURATION_NAME, ALL_RUNTIME_METADATA_CONFIGURATION_NAME).forEach { configurationName ->
            project.configurations.create(configurationName).apply {
                isCanBeConsumed = false
                isCanBeResolved = true

                usesPlatformOf(target)
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
            }
        }
    }

    private fun configureMetadataCompilation(
        target: KotlinMetadataTarget,
        sourceSet: KotlinSourceSet,
        allMetadataJar: Jar
    ): KotlinCommonCompilation {
        val project = target.project

        // With the metadata target, we publish all API dependencies of all the published source sets together:
        target.apiElementsConfiguration.extendsFrom(project.configurations.getByName(sourceSet.apiConfigurationName))

        val metadataCompilation = when (sourceSet.name) {
            // Historically, we already had a 'main' compilation in metadata targets; TODO consider removing it instead
            KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME -> target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            else -> target.compilations.create(sourceSet.name) { compilation ->
                compilation.addExactSourceSetsEagerly(setOf(sourceSet))
            }
        }

        project.addExtendsFromRelation(metadataCompilation.compileDependencyConfigurationName, ALL_COMPILE_METADATA_CONFIGURATION_NAME)

        allMetadataJar.from(metadataCompilation.output.allOutputs) { spec ->
            spec.into(metadataCompilation.defaultSourceSet.name)
        }

        project.createOrRegisterTask<TransformKotlinGranularMetadata>(transformGranularMetadataTaskName(sourceSet), listOf(sourceSet)) { }

        return metadataCompilation
    }

    private fun setupDependencyTransformationForSourceSet(
        project: Project,
        sourceSet: KotlinSourceSet,
        isSourceSetPublished: Boolean
    ) {
        KotlinDependencyScope.values().forEach { scope ->
            val allMetadataConfiguration = project.configurations.getByName(
                when (scope) {
                    KotlinDependencyScope.RUNTIME_ONLY_SCOPE -> ALL_RUNTIME_METADATA_CONFIGURATION_NAME
                    else -> ALL_COMPILE_METADATA_CONFIGURATION_NAME
                }
            )

            val granularMetadataTransformation = GranularMetadataTransformation(
                project,
                sourceSet,
                listOf(scope),
                allMetadataConfiguration,
                lazy {
                    dependsOnWithInterCompilationDependencies(project, sourceSet).filterIsInstance<DefaultKotlinSourceSet>()
                        .map { checkNotNull(it.dependencyTransformations[scope]) }
                }
            )

            (sourceSet as DefaultKotlinSourceSet).dependencyTransformations[scope] = granularMetadataTransformation

            val sourceSetMetadataConfigurationByScope = project.sourceSetMetadataConfigurationByScope(sourceSet, scope)

            granularMetadataTransformation.applyToConfiguration(sourceSetMetadataConfigurationByScope)

            val sourceSetDependencyConfigurationByScope = project.sourceSetDependencyConfigurationByScope(sourceSet, scope)

            // All source set dependencies except for compileOnly agree in versions with all other published runtime dependencies:
            if (scope != KotlinDependencyScope.COMPILE_ONLY_SCOPE) {
                if (isSourceSetPublished) {
                    project.addExtendsFromRelation(
                        ALL_RUNTIME_METADATA_CONFIGURATION_NAME,
                        sourceSetDependencyConfigurationByScope.name
                    )
                }
                project.addExtendsFromRelation(
                    sourceSetMetadataConfigurationByScope.name,
                    ALL_RUNTIME_METADATA_CONFIGURATION_NAME
                )
            }

            // All source set dependencies except for runtimeOnly agree in versions with all other published compile dependencies:
            if (scope != KotlinDependencyScope.RUNTIME_ONLY_SCOPE) {
                if (isSourceSetPublished) {
                    project.addExtendsFromRelation(
                        ALL_COMPILE_METADATA_CONFIGURATION_NAME,
                        sourceSetDependencyConfigurationByScope.name
                    )
                }
                project.addExtendsFromRelation(
                    sourceSetMetadataConfigurationByScope.name,
                    ALL_COMPILE_METADATA_CONFIGURATION_NAME
                )
            }
        }
    }

    /** Ensure that the [configuration] excludes the dependencies that are classified by this [GranularMetadataTransformation] as
     * [MetadataDependencyResolution.ExcludeAsUnrequested], and uses exactly the same versions as were resolved for the requested
     * dependencies during the transformation. */
    private fun GranularMetadataTransformation.applyToConfiguration(configuration: Configuration) {
        // Run this action immediately before the configuration first takes part in dependency resolution:
        @Suppress("UnstableApiUsage")
        configuration.withDependencies {
            val (unrequested, requested) = metadataDependencyResolutions
                .partition { it is MetadataDependencyResolution.ExcludeAsUnrequested }

            unrequested.forEach { configuration.exclude(mapOf("group" to it.dependency.moduleGroup, "module" to it.dependency.moduleName)) }

            requested.forEach {
                val notation = listOf(it.dependency.moduleGroup, it.dependency.moduleName, it.dependency.moduleVersion).joinToString(":")
                configuration.resolutionStrategy.force(notation)
            }
        }
    }

    private fun createTransformedMetadataClasspath(
        fromFiles: Iterable<File>,
        project: Project,
        transformationTaskHolders: Set<TaskHolder<TransformKotlinGranularMetadata>>
    ): FileCollection {
        return project.files(Callable {
            val allResolutionsByArtifactFile: Map<File, Iterable<MetadataDependencyResolution>> =
                mutableMapOf<File, MutableList<MetadataDependencyResolution>>().apply {
                    transformationTaskHolders.forEach {
                        val resolutions = it.doGetTask().metadataDependencyResolutions

                        resolutions.forEach { resolution ->
                            val artifacts = resolution.dependency.moduleArtifacts.map { it.file }

                            artifacts.forEach { artifactFile ->
                                getOrPut(artifactFile) { mutableListOf() }.add(resolution)
                            }
                        }
                    }
                }

            val transformedFilesByResolution: Map<MetadataDependencyResolution, FileCollection> =
                transformationTaskHolders.flatMap { it.doGetTask().filesByResolution.toList() }.toMap()

            mutableSetOf<Any /* File | FileCollection */>().apply {
                fromFiles.forEach { file ->
                    val resolutions = allResolutionsByArtifactFile[file]
                    if (resolutions == null) {
                        add(file)
                    } else {
                        val chooseVisibleSourceSets =
                            resolutions.filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()

                        if (chooseVisibleSourceSets.isNotEmpty()) {
                            add(chooseVisibleSourceSets.map { transformedFilesByResolution.getValue(it) })
                        } else if (resolutions.any { it is MetadataDependencyResolution.KeepOriginalDependency }) {
                            add(file)
                        } // else: all dependency transformations exclude this dependency as unrequested; don't add any files
                    }
                }
            }
        }).builtBy(transformationTaskHolders.map { it.getTaskOrProvider() })
    }

    private fun getPublishedCommonSourceSets(project: Project): Set<KotlinSourceSet> {
        val compilationsBySourceSet: Map<KotlinSourceSet, Set<KotlinCompilation<*>>> =
            CompilationSourceSetUtil.compilationsBySourceSets(project)

        // For now, we will only compile metadata from source sets used by multiple platforms
        // TODO once the compiler is able to analyze common code with platform-specific features and dependencies, lift this restriction
        val sourceSetsUsedInMultipleTargets = compilationsBySourceSet.filterValues { compilations ->
            compilations.map { it.target.platformType }.distinct().size > 1
        }

        // We don't want to publish source set metadata from source sets that don't participate in any compilation that is published,
        // such as test or benchmark sources; find all published compilations:
        val publishedCompilations = getPublishedPlatformCompilations(project).values

        return sourceSetsUsedInMultipleTargets
            .filterValues { compilations -> compilations.any { it in publishedCompilations } }
            .keys
    }

    private fun Project.createGenerateProjectStructureMetadataTask(): TaskHolder<GenerateProjectStructureMetadata> =
        project.createOrRegisterTask("generateProjectStructureMetadata") { task ->
            task.lazyKotlinProjectStructureMetadata = lazy { checkNotNull(buildKotlinProjectStructureMetadata(project)) }
        }
}

internal fun getPublishedPlatformCompilations(project: Project): Map<KotlinUsageContext, KotlinCompilation<*>> {
    val result = mutableMapOf<KotlinUsageContext, KotlinCompilation<*>>()

    project.multiplatformExtension.targets.withType(AbstractKotlinTarget::class.java).forEach { target ->
        if (target.platformType == KotlinPlatformType.common)
            return@forEach

        target.kotlinComponents
            .filterIsInstance<SoftwareComponentInternal>()
            .forEach { component ->
                component.usages
                    .filterIsInstance<KotlinUsageContext>()
                    .forEach { usage -> result[usage] = usage.compilation }
            }
    }

    return result
}