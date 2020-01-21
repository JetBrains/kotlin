/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.metadata

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.setArchiveAppendixCompatible
import org.jetbrains.kotlin.gradle.utils.setArchiveClassifierCompatible
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import java.io.File
import java.util.concurrent.Callable

internal const val ALL_COMPILE_METADATA_CONFIGURATION_NAME = "allSourceSetsCompileDependenciesMetadata"
internal const val ALL_RUNTIME_METADATA_CONFIGURATION_NAME = "allSourceSetsRuntimeDependenciesMetadata"

internal val Project.isKotlinGranularMetadataEnabled: Boolean
    get() = PropertiesProvider(rootProject).enableGranularSourceSetsMetadata == true

internal val Project.isCompatibilityMetadataVariantEnabled: Boolean
    get() = PropertiesProvider(this).enableCompatibilityMetadataVariant == true

class KotlinMetadataTargetConfigurator(kotlinPluginVersion: String) :
    KotlinOnlyTargetConfigurator<AbstractKotlinCompilation<*>, KotlinMetadataTarget>(
        createDefaultSourceSets = false,
        createTestCompilation = false,
        kotlinPluginVersion = kotlinPluginVersion
    ) {
    companion object {
        internal const val ALL_METADATA_JAR_NAME = "allMetadataJar"

        internal fun transformGranularMetadataTaskName(sourceSetName: String) =
            lowerCamelCaseName("transform", sourceSetName, "DependenciesMetadata")

        // TODO generalize once a general production-test and other kinds of inter-compilation visibility are supported
        // Currently, this is a temporary ad-hoc mechanism for exposing the commonMain dependencies to the test source sets
        internal fun dependsOnWithInterCompilationDependencies(project: Project, sourceSet: KotlinSourceSet): Set<KotlinSourceSet> =
            sourceSet.dependsOn.toMutableSet().apply {
                if (sourceSet.name == KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)
                    add(project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
            }
    }

    override fun configureTarget(target: KotlinMetadataTarget) {
        super.configureTarget(target)

        if (target.project.isKotlinGranularMetadataEnabled) {
            KotlinBuildStatsService.getInstance()?.report(BooleanMetrics.ENABLED_HMPP, true)

            target.compilations.withType(KotlinCommonCompilation::class.java).getByName(KotlinCompilation.MAIN_COMPILATION_NAME).run {
                if (target.project.isCompatibilityMetadataVariantEnabled) {
                    // Force the default 'main' compilation to produce *.kotlin_metadata regardless of the klib feature flag.
                    forceCompilationToKotlinMetadata = true
                } else {
                    // Clear the dependencies of the compilation so that they don't take time resolving during task graph construction:
                    compileDependencyFiles = target.project.files()
                }
                compileKotlinTaskHolder.configure { it.onlyIf { target.project.isCompatibilityMetadataVariantEnabled } }
            }

            createMergedAllSourceSetsConfigurations(target)

            val allMetadataJar = target.project.tasks.getByName(ALL_METADATA_JAR_NAME) as Jar
            createMetadataCompilationsForCommonSourceSets(target, allMetadataJar)

            configureProjectStructureMetadataGeneration(allMetadataJar)

            setupDependencyTransformationForCommonSourceSets(target)

            target.project.configurations.getByName(target.apiElementsConfigurationName).attributes
                .attribute(USAGE_ATTRIBUTE, target.project.usageByName(KotlinUsages.KOTLIN_METADATA))

            if (target.project.isCompatibilityMetadataVariantEnabled) {
                createCommonMainElementsConfiguration(target)
            }
        }
    }

    override fun setupCompilationDependencyFiles(compilation: KotlinCompilation<KotlinCommonOptions>) {
        val project = compilation.target.project

        /** See [createTransformedMetadataClasspath] and its usage. */
        if (project.isKotlinGranularMetadataEnabled && compilation.name != KotlinCompilation.MAIN_COMPILATION_NAME)
            compilation.compileDependencyFiles = project.files()
        else
            super.setupCompilationDependencyFiles(compilation)
    }

    override fun buildCompilationProcessor(compilation: AbstractKotlinCompilation<*>): KotlinCompilationProcessor<*> = when (compilation) {
        is KotlinCommonCompilation -> {
            val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
            KotlinCommonSourceSetProcessor(compilation, tasksProvider, kotlinPluginVersion)
        }
        is KotlinSharedNativeCompilation -> NativeSharedCompilationProcessor(compilation)
        else -> error("unsupported compilation type ${compilation::class.qualifiedName}")
    }

    private inner class NativeSharedCompilationProcessor(
        override val kotlinCompilation: KotlinSharedNativeCompilation
    ) : KotlinCompilationProcessor<KotlinNativeCompile>(kotlinCompilation) {

        private val nativeTargetConfigurator = KotlinNativeTargetConfigurator<KotlinNativeTarget>(kotlinPluginVersion)

        override val kotlinTask: TaskProvider<out KotlinNativeCompile> =
            with(nativeTargetConfigurator) {
                project.createKlibCompilationTask(kotlinCompilation)
            }

        override fun run() = Unit
    }

    override fun createJarTasks(target: KotlinMetadataTarget): Pair<String, Zip> {
        val result = super.createJarTasks(target)

        if (target.project.isKotlinGranularMetadataEnabled) {
            target.project.locateTask<Jar>(target.artifactsTaskName)!!.configure {
                if (!target.project.isCompatibilityMetadataVariantEnabled) {
                    it.setArchiveClassifierCompatible { "commonMain" }
                }
                it.onlyIf { target.project.isCompatibilityMetadataVariantEnabled }
            }

            /** This JAR is created in addition to the main one, published with a classifier, but is by default used
             * for project dependencies (as the Kotlin Granular metadata is enabled across all projects in a build, this is OK).
             * See also [KotlinMetadataTarget.kotlinComponents]
             */
            target.project.registerTask<Jar>(ALL_METADATA_JAR_NAME) { allMetadataJar ->
                allMetadataJar.description = "Assembles a jar archive containing the metadata for all Kotlin source sets."
                allMetadataJar.group = BasePlugin.BUILD_GROUP

                allMetadataJar.setArchiveAppendixCompatible { target.name.toLowerCase() }

                if (target.project.isCompatibilityMetadataVariantEnabled) {
                    allMetadataJar.setArchiveClassifierCompatible { "all" }
                }
            }
        }

        return result
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

        val sourceSetsWithMetadataCompilations: Map<KotlinSourceSet, AbstractKotlinCompilation<*>> =
            publishedCommonSourceSets.associate { sourceSet ->
                sourceSet to createMetadataCompilation(target, sourceSet, allMetadataJar)
            }

        if (project.isCompatibilityMetadataVariantEnabled) {
            val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            configureMetadataDependenciesForCompilation(mainCompilation)
        }

        sourceSetsWithMetadataCompilations.values.forEach { compilation ->
            exportDependenciesForPublishing(compilation)
        }
    }

    private fun configureProjectStructureMetadataGeneration(allMetadataJar: Jar): AbstractCopyTask {
        val project = allMetadataJar.project
        val generateMetadata = project.createGenerateProjectStructureMetadataTask()

        return allMetadataJar.from(
            project.files(Callable { generateMetadata.get().resultXmlFile }).builtBy(generateMetadata)
        ) { spec ->
            spec.into("META-INF").rename { MULTIPLATFORM_PROJECT_METADATA_FILE_NAME }
        }
    }

    private fun exportDependenciesForPublishing(
        compilation: AbstractKotlinCompilation<*>
    ) {
        val sourceSet = compilation.defaultSourceSet
        val isSharedNativeCompilation = compilation is KotlinSharedNativeCompilation
        val target = compilation.target
        with(target.project) {
            val apiElementsConfiguration = configurations.getByName(target.apiElementsConfigurationName)

            // With the metadata target, we publish all API dependencies of all the published source sets together:
            apiElementsConfiguration.extendsFrom(sourceSetDependencyConfigurationByScope(sourceSet, KotlinDependencyScope.API_SCOPE))

            /** For Kotlin/Native-shared source sets, we also add the implementation dependencies to apiElements, because Kotlin/Native
             * can't have any implementation dependencies, all dependencies used for compilation must be shipped along with the klib.
             * It's OK that they are exposed as API dependencies here, because at the consumer side, they are dispatched to the
             * consumer's source sets in a granular way, so they will only be visible in the source sets that see the sharedn-Native
             * source set.
             * See also: [buildKotlinProjectStructureMetadata], where these dependencies must be included into the source set exported deps.
             */
            if (isSharedNativeCompilation) {
                sourceSet.getSourceSetHierarchy().forEach { hierarchySourceSet ->
                    apiElementsConfiguration.extendsFrom(
                        sourceSetDependencyConfigurationByScope(hierarchySourceSet, KotlinDependencyScope.IMPLEMENTATION_SCOPE)
                    )
                }
            }
        }
    }

    private fun createMergedAllSourceSetsConfigurations(target: KotlinMetadataTarget): Unit = with(target.project) {
        listOf(ALL_COMPILE_METADATA_CONFIGURATION_NAME, ALL_RUNTIME_METADATA_CONFIGURATION_NAME).forEach { configurationName ->
            project.configurations.create(configurationName).apply {
                isCanBeConsumed = false
                isCanBeResolved = true

                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
            }
        }
    }

    private fun createMetadataCompilation(
        target: KotlinMetadataTarget,
        sourceSet: KotlinSourceSet,
        allMetadataJar: Jar
    ): AbstractKotlinCompilation<*> {
        val project = target.project

        val compilationName = sourceSet.name

        val isNativeSourceSet = CompilationSourceSetUtil.compilationsBySourceSets(project).getValue(sourceSet)
            .all { compilation -> compilation.target is KotlinNativeTarget }

        val compilationFactory: KotlinCompilationFactory<out AbstractKotlinCompilation<*>> = when {
            isNativeSourceSet -> KotlinSharedNativeCompilationFactory(target)
            else -> KotlinCommonCompilationFactory(target)
        }

        return compilationFactory.create(compilationName).apply {
            target.compilations.add(this@apply)
            addExactSourceSetsEagerly(setOf(sourceSet))

            configureMetadataDependenciesForCompilation(this@apply)

            val metadataContent = project.filesWithUnpackedArchives(this@apply.output.allOutputs, setOf("klib"))
            allMetadataJar.from(metadataContent) { spec -> spec.into(this@apply.defaultSourceSet.name) }
        }
    }

    private fun configureMetadataDependenciesForCompilation(compilation: AbstractKotlinCompilation<*>) {
        val project = compilation.target.project
        val sourceSet = compilation.defaultSourceSet

        project.registerTask<TransformKotlinGranularMetadata>(
            transformGranularMetadataTaskName(compilation.name),
            listOf(sourceSet)
        ) { }

        compilation.compileDependencyFiles += createTransformedMetadataClasspath(
            project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME),
            compilation
        )
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
        compilation: AbstractKotlinCompilation<*>
    ): FileCollection {
        val project = compilation.target.project

        // Adjust metadata compilation to support source set hierarchies, i.e. use both the outputs of dependsOn source set compilation
        // and their dependencies metadata transformed for compilation:
        return project.files(
            project.provider {
                val metadataTarget = compilation.target
                val sourceSet = compilation.defaultSourceSet

                val transformationTaskHolders = sourceSet.getSourceSetHierarchy().mapNotNull { hierarchySourceSet ->
                    project.locateTask<TransformKotlinGranularMetadata>(transformGranularMetadataTaskName(hierarchySourceSet.name))
                }

                val allResolutionsByArtifactFile: Map<File, Iterable<MetadataDependencyResolution>> =
                    mutableMapOf<File, MutableList<MetadataDependencyResolution>>().apply {
                        transformationTaskHolders.forEach {
                            val resolutions = it.get().metadataDependencyResolutions

                            resolutions.forEach { resolution ->
                                val artifacts = resolution.dependency.moduleArtifacts.map { it.file }

                                artifacts.forEach { artifactFile ->
                                    getOrPut(artifactFile) { mutableListOf() }.add(resolution)
                                }
                            }
                        }
                    }

                val transformedFilesByResolution: Map<MetadataDependencyResolution, FileCollection> =
                    transformationTaskHolders.flatMap { it.get().filesByResolution.toList() }.toMap()

                val dependsOnCompilationOutputs = sourceSet.getSourceSetHierarchy().mapNotNull { hierarchySourceSet ->
                    val dependencyCompilation = metadataTarget.compilations.getByName(hierarchySourceSet.name)
                    dependencyCompilation.output.classesDirs.takeIf { hierarchySourceSet != sourceSet }
                }

                mutableSetOf<Any /* File | FileCollection */>().apply {

                    addAll(dependsOnCompilationOutputs)

                    fromFiles.forEach { file ->
                        val resolutions = allResolutionsByArtifactFile[file]
                        if (resolutions == null) {
                            add(file)
                        } else {
                            val chooseVisibleSourceSets =
                                resolutions.filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()

                            if (chooseVisibleSourceSets.isNotEmpty()) {
                                // Wrap the list into a FileCollection, as some older Gradle version failed to resolve the classpath
                                add(project.files(chooseVisibleSourceSets.map { transformedFilesByResolution.getValue(it) }))
                            } else if (resolutions.any { it is MetadataDependencyResolution.KeepOriginalDependency }) {
                                add(file)
                            } // else: all dependency transformations exclude this dependency as unrequested; don't add any files
                        }
                    }

                    // Add a build dependency on the granular metadata transformations of the dependency source sets
                    add(project.files().builtBy(transformationTaskHolders))
                }
            }
        )
    }

    private fun createCommonMainElementsConfiguration(target: KotlinMetadataTarget) {
        val project = target.project
        project.configurations.create(COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME).apply {
            isCanBeConsumed = true
            isCanBeResolved = false
            usesPlatformOf(target)

            attributes.attribute(
                USAGE_ATTRIBUTE,
                KotlinUsages.producerApiUsage(target)
            ) // 'kotlin-api' usage

            val commonMainApiConfiguration = project.sourceSetDependencyConfigurationByScope(
                project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME),
                KotlinDependencyScope.API_SCOPE
            )
            extendsFrom(commonMainApiConfiguration)

            project.artifacts.add(name, project.tasks.getByName(target.artifactsTaskName))
        }
    }

    private fun getPublishedCommonSourceSets(project: Project): Set<KotlinSourceSet> {
        val compilationsBySourceSet: Map<KotlinSourceSet, Set<KotlinCompilation<*>>> =
            CompilationSourceSetUtil.compilationsBySourceSets(project)

        val sourceSetsUsedInMultipleTargets = compilationsBySourceSet.filterValues { compilations ->
            compilations.map { it.target.platformType }.distinct().run {
                size > 1 && toSet() != setOf(KotlinPlatformType.androidJvm, KotlinPlatformType.jvm) || (
                        singleOrNull() == KotlinPlatformType.native &&
                                compilations.map { it.target }.distinct().size > 1 &&
                                compilations.any { (it as AbstractKotlinNativeCompilation).konanTarget.enabledOnCurrentHost }
                        )
                // TODO: platform-shared source sets other than Kotlin/Native ones are not yet supported; support will be needed for JVM, JS
            }
        }

        // We don't want to publish source set metadata from source sets that don't participate in any compilation that is published,
        // such as test or benchmark sources; find all published compilations:
        val publishedCompilations = getPublishedPlatformCompilations(project).values

        return sourceSetsUsedInMultipleTargets
            .filterValues { compilations -> compilations.any { it in publishedCompilations } }
            .keys
    }

    private fun Project.createGenerateProjectStructureMetadataTask(): TaskProvider<GenerateProjectStructureMetadata> =
        project.registerTask("generateProjectStructureMetadata") { task ->
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

private fun Project.filesWithUnpackedArchives(from: FileCollection, extensions: Set<String>): FileCollection =
    project.files(project.provider {
        from.map {
            if (it.extension in extensions)
                project.zipTree(it)
            else it
        }
    }).builtBy(from)