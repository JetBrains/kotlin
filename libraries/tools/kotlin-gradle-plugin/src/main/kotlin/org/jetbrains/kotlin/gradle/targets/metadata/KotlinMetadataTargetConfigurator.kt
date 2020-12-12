/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.metadata

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil.compilationsBySourceSets
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics

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

        internal fun dependsOnWithInterCompilationDependencies(project: Project, sourceSet: KotlinSourceSet): Set<KotlinSourceSet> =
            sourceSet.dependsOn.toMutableSet().apply {
                addAll(getVisibleSourceSetsFromAssociateCompilations(project, sourceSet))
            }
    }

    override fun configureTarget(target: KotlinMetadataTarget) {
        super.configureTarget(target)

        if (target.project.isKotlinGranularMetadataEnabled) {
            KotlinBuildStatsService.getInstance()?.report(BooleanMetrics.ENABLED_HMPP, true)

            target.compilations.withType(KotlinCommonCompilation::class.java).getByName(KotlinCompilation.MAIN_COMPILATION_NAME).run {
                // Capture it here to use in onlyIf spec. Direct usage causes serialization of target attempt when configuration cache is enabled
                val isCompatibilityMetadataVariantEnabled = target.project.isCompatibilityMetadataVariantEnabled
                if (isCompatibilityMetadataVariantEnabled) {
                    // Force the default 'main' compilation to produce *.kotlin_metadata regardless of the klib feature flag.
                    forceCompilationToKotlinMetadata = true
                } else {
                    // Clear the dependencies of the compilation so that they don't take time resolving during task graph construction:
                    compileDependencyFiles = target.project.files()
                }
                compileKotlinTaskProvider.configure { it.onlyIf { isCompatibilityMetadataVariantEnabled } }
            }

            createMergedAllSourceSetsConfigurations(target)

            val allMetadataJar = target.project.tasks.withType<Jar>().named(ALL_METADATA_JAR_NAME)
            createMetadataCompilationsForCommonSourceSets(target, allMetadataJar)

            configureProjectStructureMetadataGeneration(target.project, allMetadataJar)

            setupDependencyTransformationForCommonSourceSets(target)

            target.project.configurations.getByName(target.apiElementsConfigurationName).run {
                attributes.attribute(USAGE_ATTRIBUTE, target.project.usageByName(KotlinUsages.KOTLIN_METADATA))
                /** Note: to add this artifact here is enough to avoid duplicate artifacts in this configuration: the default artifact
                 * won't be added (later) if there's already an artifact in the configuration, see
                 * [KotlinOnlyTargetConfigurator.configureArchivesAndComponent] */
                target.project.artifacts.add(target.apiElementsConfigurationName, allMetadataJar)
            }

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

    override fun createArchiveTasks(target: KotlinMetadataTarget): TaskProvider<out Zip> {
        if (!target.project.isKotlinGranularMetadataEnabled)
            return super.createArchiveTasks(target)

        val result = target.project.registerTask<Jar>(target.artifactsTaskName) {
            it.group = BasePlugin.BUILD_GROUP
            /** The content is added to this JAR in [KotlinMetadataTargetConfigurator.configureTarget]. */
        }

        result.configure { allMetadataJar ->
            allMetadataJar.description = "Assembles a jar archive containing the metadata for all Kotlin source sets."
            allMetadataJar.group = BasePlugin.BUILD_GROUP

            if (target.project.isCompatibilityMetadataVariantEnabled) {
                allMetadataJar.archiveClassifier.set("all")
            }
        }

        val legacyJar = target.project.registerTask<Jar>(target.legacyArtifactsTaskName)
        legacyJar.configure {
            // Capture it here to use in onlyIf spec. Direct usage causes serialization of target attempt when configuration cache is enabled
            val isCompatibilityMetadataVariantEnabled = target.project.isCompatibilityMetadataVariantEnabled
            it.description = "Assembles an archive containing the Kotlin metadata of the commonMain source set."
            if (!isCompatibilityMetadataVariantEnabled) {
                it.archiveClassifier.set("commonMain")
            }
            it.onlyIf { isCompatibilityMetadataVariantEnabled }
            it.from(target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).output.allOutputs)
        }

        return result
    }

    private fun setupDependencyTransformationForCommonSourceSets(target: KotlinMetadataTarget) {
        target.project.whenEvaluated {
            val publishedCommonSourceSets: Set<KotlinSourceSet> = getCommonSourceSetsForMetadataCompilation(project)

            kotlinExtension.sourceSets.all {
                setupDependencyTransformationForSourceSet(target.project, it, it in publishedCommonSourceSets)
            }
        }
    }

    private fun createMetadataCompilationsForCommonSourceSets(
        target: KotlinMetadataTarget,
        allMetadataJar: TaskProvider<out Jar>
    ) = target.project.whenEvaluated {
        // Do this after all targets are configured by the user build script

        val publishedCommonSourceSets: Set<KotlinSourceSet> = getCommonSourceSetsForMetadataCompilation(project)
        val hostSpecificSourceSets: Set<KotlinSourceSet> = getHostSpecificSourceSets(project).toSet()

        val sourceSetsWithMetadataCompilations: Map<KotlinSourceSet, AbstractKotlinCompilation<*>> = publishedCommonSourceSets
            .associateWith { sourceSet ->
                createMetadataCompilation(target, sourceSet, allMetadataJar, sourceSet in hostSpecificSourceSets)
            }
            .onEach { (sourceSet, compilation) ->
                if (!isMetadataCompilationSupported(target.project, sourceSet)) {
                    compilation.compileKotlinTaskProvider.configure { it.enabled = false }
                }
            }

        if (project.isCompatibilityMetadataVariantEnabled) {
            val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            configureMetadataDependenciesForCompilation(mainCompilation)
        }

        sourceSetsWithMetadataCompilations.values.forEach { compilation ->
            exportDependenciesForPublishing(compilation)
        }
    }

    private fun isMetadataCompilationSupported(project: Project, sourceSet: KotlinSourceSet): Boolean {
        val platforms = compilationsBySourceSets(project)[sourceSet].orEmpty()
            .filter { it.target !is KotlinMetadataTarget }
            .map { it.target.platformType }.distinct()

        /*
        Android and jvm do share the JVM backend which is not supported for metadata compilation
        See [HMPP: Bad IDEA dependencies for JVM and Android intermediate source set](https://youtrack.jetbrains.com/issue/KT-42383)
        See [HMPP: JVM and Android intermediate source set publication](https://youtrack.jetbrains.com/issue/KT-42468)
        */
        if (platforms.all { it == KotlinPlatformType.jvm || it == KotlinPlatformType.androidJvm }) {
            return false
        }

        /* Metadata compilation for a single platform is only supported native and common source sets */
        if (platforms.size == 1) {
            val platform = platforms.single()
            return platform == KotlinPlatformType.native || platform == KotlinPlatformType.common
        }

        /* Source sets sharing code between multiple backends are supported */
        return true
    }

    private fun configureProjectStructureMetadataGeneration(project: Project, allMetadataJar: TaskProvider<out Jar>) {
        val generateMetadata = project.createGenerateProjectStructureMetadataTask()

        allMetadataJar.configure {
            it.from(generateMetadata.map { it.resultXmlFile }) { spec ->
                spec.into("META-INF").rename { MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME }
            }
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
        allMetadataJar: TaskProvider<out Jar>,
        isHostSpecific: Boolean
    ): AbstractKotlinCompilation<*> {
        val project = target.project

        val compilationName = sourceSet.name

        val platformCompilations = compilationsBySourceSets(project).getValue(sourceSet)

        val isNativeSourceSet = platformCompilations.all { compilation -> compilation.target is KotlinNativeTarget }

        val compilationFactory: KotlinCompilationFactory<out AbstractKotlinCompilation<*>> = when {
            isNativeSourceSet -> KotlinSharedNativeCompilationFactory(
                target,
                platformCompilations.map { (it as AbstractKotlinNativeCompilation).konanTarget }
            )
            else -> KotlinCommonCompilationFactory(target)
        }

        return compilationFactory.create(compilationName).apply {
            target.compilations.add(this@apply)
            addExactSourceSetsEagerly(setOf(sourceSet))
            configureMetadataDependenciesForCompilation(this@apply)

            if (!isHostSpecific) {
                val metadataContent = project.filesWithUnpackedArchives(this@apply.output.allOutputs, setOf("klib"))
                allMetadataJar.configure { it.from(metadataContent) { spec -> spec.into(this@apply.defaultSourceSet.name) } }
            } else {
                if (platformCompilations.filterIsInstance<KotlinNativeCompilation>().none { it.konanTarget.enabledOnCurrentHost }) {
                    // Then we don't have any platform module to put this compiled source set to, so disable the compilation task:
                    compileKotlinTaskProvider.configure { it.enabled = false }
                    // Also clear the dependency files (classpath) of the compilation so that the host-specific dependencies are
                    // not resolved:
                    compileDependencyFiles = project.files()
                }
            }
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

            unrequested.forEach {
                val (group, name) = it.projectDependency?.run {
                    /** Note: the project dependency notation here should be exactly this, group:name,
                     * not from [ModuleIds.fromProjectPathDependency], as `exclude` checks it against the project's group:name  */
                    ModuleDependencyIdentifier(group.toString(), name)
                } ?: ModuleIds.fromComponent(project, it.dependency)
                configuration.exclude(mapOf("group" to group, "module" to name))
            }

            requested.filter { it.projectDependency == null }.forEach {
                val (group, name) = ModuleIds.fromComponent(project, it.dependency)
                val notation = listOfNotNull(group.orEmpty(), name, it.dependency.moduleVersion?.version).joinToString(":")
                configuration.resolutionStrategy.force(notation)
            }
        }
    }

    private fun createTransformedMetadataClasspath(
        fromFiles: Configuration,
        compilation: AbstractKotlinCompilation<*>
    ): FileCollection {
        val project = compilation.target.project

        // Adjust metadata compilation to support source set hierarchies, i.e. use both the outputs of dependsOn source set compilation
        // and their dependencies metadata transformed for compilation:
        return project.files(
            project.provider {
                val sourceSet = compilation.defaultSourceSet

                val transformationTaskHolders = sourceSet.getSourceSetHierarchy().mapNotNull { hierarchySourceSet ->
                    project.locateTask<TransformKotlinGranularMetadata>(transformGranularMetadataTaskName(hierarchySourceSet.name))
                }

                val allResolutionsByComponentId: Map<ComponentIdentifier, List<MetadataDependencyResolution>> =
                    mutableMapOf<ComponentIdentifier, MutableList<MetadataDependencyResolution>>().apply {
                        transformationTaskHolders.forEach {
                            val resolutions = it.get().metadataDependencyResolutions
                            resolutions.forEach { resolution ->
                                getOrPut(resolution.dependency.id) { mutableListOf() }.add(resolution)
                            }
                        }
                    }

                val transformedFilesByResolution: Map<MetadataDependencyResolution, FileCollection> =
                    transformationTaskHolders.flatMap { it.get().filesByResolution.toList() }.toMap()

                val dependsOnCompilationOutputs = sourceSet.getSourceSetHierarchy().mapNotNull { hierarchySourceSet ->
                    val dependencyCompilation = project.getMetadataCompilationForSourceSet(hierarchySourceSet)
                    dependencyCompilation?.output?.classesDirs.takeIf { hierarchySourceSet != sourceSet }
                }

                val artifactView = fromFiles.incoming.artifactView { view ->
                    view.componentFilter { id ->
                        allResolutionsByComponentId[id].let { resolutions ->
                            resolutions == null || resolutions.any { it !is MetadataDependencyResolution.ExcludeAsUnrequested }
                        }
                    }
                }

                mutableSetOf<Any /* File | FileCollection */>().apply {
                    addAll(dependsOnCompilationOutputs)
                    artifactView.artifacts.forEach { artifact ->
                        val resolutions = allResolutionsByComponentId[artifact.id.componentIdentifier]
                        if (resolutions == null) {
                            add(artifact.file)
                        } else {
                            val chooseVisibleSourceSets =
                                resolutions.filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()

                            if (chooseVisibleSourceSets.isNotEmpty()) {
                                // Wrap the list into a FileCollection, as some older Gradle version failed to resolve the classpath
                                add(project.files(chooseVisibleSourceSets.map { transformedFilesByResolution.getValue(it) }))
                            } else if (resolutions.any { it is MetadataDependencyResolution.KeepOriginalDependency }) {
                                add(artifact.file)
                            } // else: all dependency transformations exclude this dependency as unrequested; don't add any files
                        }
                    }

                    // Add a build dependency on the granular metadata transformations of the dependency source sets
                    add(project.files().builtBy(transformationTaskHolders))
                }
            }
        )
        /*
        val transformedFilesByOriginalFiles = project.provider {
            transformationTaskHolders
                .flatMap { it.get().filesByOriginalFiles.toList() }
                .groupBy({ it.first }, valueTransform = { it.second })
        }

        val builtBySet = mutableSetOf<FileCollection>()
        val resultFiles = project.files(Callable {
            val originalFiles = fromFiles.toSet()
            val filesToAdd = mutableSetOf<File>()
            val filesToExclude = mutableSetOf<File>()

            transformedFilesByOriginalFiles.get().forEach { (original, replacement) ->
                if (original.all { it in originalFiles }) {
                    builtBySet.addAll(replacement)
                    filesToAdd += replacement.flatMap { it.files }
                    filesToExclude += original
                }
            }

            originalFiles - filesToExclude + filesToAdd
        })
        return resultFiles.builtBy(builtBySet)
         */
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

            project.artifacts.add(name, project.tasks.getByName(target.legacyArtifactsTaskName))
        }
    }

    private fun Project.createGenerateProjectStructureMetadataTask(): TaskProvider<GenerateProjectStructureMetadata> =
        project.registerTask("generateProjectStructureMetadata") { task ->
            task.lazyKotlinProjectStructureMetadata = lazy { checkNotNull(buildKotlinProjectStructureMetadata(project)) }
        }
}

/**
 * @return All common source sets that can potentially be published. Right now, not all combinations of platforms actually
 * support metadata compilation (see [KotlinMetadataTargetConfigurator.isMetadataCompilationSupported].
 * Those compilations will be created but the corresponding tasks will be disabled.
 */
internal fun getCommonSourceSetsForMetadataCompilation(project: Project): Set<KotlinSourceSet> {
    val compilationsBySourceSet: Map<KotlinSourceSet, Set<KotlinCompilation<*>>> =
        compilationsBySourceSets(project)

    val sourceSetsUsedInMultipleTargets = compilationsBySourceSet.filterValues { compilations ->
        compilations.map { it.target.platformType }.distinct().run {
            size > 1 || singleOrNull() == KotlinPlatformType.native && compilations.map { it.target }.distinct().size > 1
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
                    .filter { it.includeIntoProjectStructureMetadata }
                    .forEach { usage -> result[usage] = usage.compilation }
            }
    }

    return result
}

internal fun Project.filesWithUnpackedArchives(from: FileCollection, extensions: Set<String>): FileCollection =
    project.files(project.provider {
        from.mapNotNull {
            if (it.extension in extensions) {
                if (it.exists()) project.zipTree(it) else null
            } else it
        }
    }).builtBy(from)

internal fun Project.getMetadataCompilationForSourceSet(sourceSet: KotlinSourceSet): AbstractKotlinCompilation<*>? {
    return multiplatformExtension.metadata().compilations.findByName(sourceSet.name)
}
