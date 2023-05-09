/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.metadata

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.pm20ExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropMetadataDependencyClasspath
import org.jetbrains.kotlin.gradle.targets.native.internal.includeCommonizedCInteropMetadata
import org.jetbrains.kotlin.gradle.targets.native.internal.sharedCommonizerTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

internal const val COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME = "commonMainMetadataElements"

internal val Project.isKotlinGranularMetadataEnabled: Boolean
    get() = project.pm20ExtensionOrNull != null || with(PropertiesProvider(this)) {
        mppHierarchicalStructureByDefault || // then we want to use KLIB granular compilation & artifacts even if it's just commonMain
                hierarchicalStructureSupport ||
                enableGranularSourceSetsMetadata == true
    }

internal val Project.shouldCompileIntermediateSourceSetsToMetadata: Boolean
    get() = project.pm20ExtensionOrNull != null || with(PropertiesProvider(this)) {
        when {
            !hierarchicalStructureSupport && mppHierarchicalStructureByDefault -> false
            else -> true
        }
    }

internal val Project.isCompatibilityMetadataVariantEnabled: Boolean
    get() = PropertiesProvider(this).enableCompatibilityMetadataVariant == true

class KotlinMetadataTargetConfigurator :
    KotlinOnlyTargetConfigurator<KotlinCompilation<*>, KotlinMetadataTarget>(createTestCompilation = false) {
    companion object {
        internal const val ALL_METADATA_JAR_NAME = "allMetadataJar"
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
                    // Add directly dependsOn sources for Legacy Compatibility Metadata variant
                    // it isn't necessary for KLib compilations
                    // see [KotlinCompilationSourceSetInclusion.AddSourcesWithoutDependsOnClosure]
                    defaultSourceSet.internal.dependsOnClosure.forAll {
                        source(it)
                    }
                } else {
                    // Clear the dependencies of the compilation so that they don't take time resolving during task graph construction:
                    compileDependencyFiles = target.project.files()
                }
                compileKotlinTaskProvider.configure { it.onlyIf { isCompatibilityMetadataVariantEnabled } }
            }

            val allMetadataJar = target.project.tasks.named<Jar>(ALL_METADATA_JAR_NAME)
            createMetadataCompilationsForCommonSourceSets(target, allMetadataJar)

            configureProjectStructureMetadataGeneration(target.project, allMetadataJar)

            configureMetadataDependenciesConfigurationsForCommonSourceSets(target)

            target.project.configurations.getByName(target.apiElementsConfigurationName).run {
                attributes.attribute(USAGE_ATTRIBUTE, target.project.usageByName(KotlinUsages.KOTLIN_METADATA))
                attributes.attribute(CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))
                /** Note: to add this artifact here is enough to avoid duplicate artifacts in this configuration: the default artifact
                 * won't be added (later) if there's already an artifact in the configuration, see
                 * [KotlinOnlyTargetConfigurator.configureArchivesAndComponent] */
                target.project.artifacts.add(target.apiElementsConfigurationName, allMetadataJar)
            }

            if (target.project.isCompatibilityMetadataVariantEnabled) {
                createCommonMainElementsConfiguration(target)
            }
        } else {
            /* We had nothing to do: Still mark this job as complete */
            target.metadataCompilationsCreated.complete()
        }
    }

    override fun setupCompilationDependencyFiles(compilation: KotlinCompilation<KotlinCommonOptions>) {
        val project = compilation.target.project

        /** See [configureMetadataDependenciesForCompilation] */
        if (project.isKotlinGranularMetadataEnabled && compilation.name != KotlinCompilation.MAIN_COMPILATION_NAME)
            compilation.compileDependencyFiles = project.files()
        else
            super.setupCompilationDependencyFiles(compilation)
    }

    override fun buildCompilationProcessor(compilation: KotlinCompilation<*>): KotlinCompilationProcessor<*> = when (compilation) {
        is KotlinCommonCompilation -> {
            val tasksProvider = KotlinTasksProvider()
            KotlinCommonSourceSetProcessor(KotlinCompilationInfo(compilation), tasksProvider)
        }

        is KotlinSharedNativeCompilation -> NativeSharedCompilationProcessor(compilation)
        else -> error("unsupported compilation type ${compilation::class.qualifiedName}")
    }

    override fun createArchiveTasks(target: KotlinMetadataTarget): TaskProvider<out Zip> {
        if (!target.project.isKotlinGranularMetadataEnabled)
            return super.createArchiveTasks(target)

        val result = target.project.registerTask<Jar>(target.artifactsTaskName) {
            it.group = BasePlugin.BUILD_GROUP
            it.isReproducibleFileOrder = true
            it.isPreserveFileTimestamps = false
            /** The content is added to this JAR in [KotlinMetadataTargetConfigurator.configureTarget]. */
        }

        result.configure { allMetadataJar ->
            allMetadataJar.description = "Assembles a jar archive containing the metadata for all Kotlin source sets."
            allMetadataJar.group = BasePlugin.BUILD_GROUP

            if (target.project.isCompatibilityMetadataVariantEnabled) {
                allMetadataJar.archiveClassifier.set("all")
            }
        }

        if (target.project.isCompatibilityMetadataVariantEnabled) {
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
        }

        return result
    }

    private fun configureMetadataDependenciesConfigurationsForCommonSourceSets(target: KotlinMetadataTarget) {
        target.project.whenEvaluated {
            kotlinExtension.sourceSets.all { sourceSet ->
                // Resolvable metadata configuration must be initialized for all source sets
                // As it configures legacy metadata configurations that is used by older IDE Import
                // And it also configures platform source sets for the same reason
                sourceSet.internal.resolvableMetadataConfiguration
            }
        }
    }

    private fun createMetadataCompilationsForCommonSourceSets(
        target: KotlinMetadataTarget,
        allMetadataJar: TaskProvider<out Jar>
    ) = target.project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseDsl) {
        withRestrictedStages(KotlinPluginLifecycle.Stage.upTo(KotlinPluginLifecycle.Stage.FinaliseCompilations)) {
            // Do this after all targets are configured by the user build script

            val publishedCommonSourceSets: Set<KotlinSourceSet> = getCommonSourceSetsForMetadataCompilation(project)
            val hostSpecificSourceSets: Set<KotlinSourceSet> = getHostSpecificSourceSets(project).toSet()

            val sourceSetsWithMetadataCompilations: Map<KotlinSourceSet, KotlinCompilation<*>> = publishedCommonSourceSets
                .associateWith { sourceSet ->
                    createMetadataCompilation(target, sourceSet, allMetadataJar, sourceSet in hostSpecificSourceSets)
                }
                .onEach { (sourceSet, compilation) ->
                    if (!isMetadataCompilationSupported(sourceSet)) {
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

            target.metadataCompilationsCreated.complete()
        }
    }

    private suspend fun isMetadataCompilationSupported(sourceSet: KotlinSourceSet): Boolean {
        val platforms = sourceSet.internal.awaitPlatformCompilations()
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
            it.from(generateMetadata.map { it.resultFile }) { spec ->
                spec.into("META-INF").rename { MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME }
            }
        }
    }

    private fun exportDependenciesForPublishing(
        compilation: KotlinCompilation<*>
    ) {
        val sourceSet = compilation.defaultSourceSet
        val isSharedNativeCompilation = compilation is KotlinSharedNativeCompilation
        val target = compilation.target
        with(target.project) {
            val apiElementsConfiguration = configurations.getByName(target.apiElementsConfigurationName)

            // With the metadata target, we publish all API dependencies of all the published source sets together:
            apiElementsConfiguration.extendsFrom(
                configurations.sourceSetDependencyConfigurationByScope(
                    sourceSet,
                    KotlinDependencyScope.API_SCOPE
                )
            )

            /** For Kotlin/Native-shared source sets, we also add the implementation dependencies to apiElements, because Kotlin/Native
             * can't have any implementation dependencies, all dependencies used for compilation must be shipped along with the klib.
             * It's OK that they are exposed as API dependencies here, because at the consumer side, they are dispatched to the
             * consumer's source sets in a granular way, so they will only be visible in the source sets that see the sharedn-Native
             * source set.
             * See also: [buildKotlinProjectStructureMetadata], where these dependencies must be included into the source set exported deps.
             */
            if (isSharedNativeCompilation) {
                sourceSet.internal.withDependsOnClosure.forEach { hierarchySourceSet ->
                    apiElementsConfiguration.extendsFrom(
                        configurations.sourceSetDependencyConfigurationByScope(
                            hierarchySourceSet,
                            KotlinDependencyScope.IMPLEMENTATION_SCOPE
                        )
                    )
                }
            }
        }
    }

    private suspend fun createMetadataCompilation(
        target: KotlinMetadataTarget,
        sourceSet: KotlinSourceSet,
        allMetadataJar: TaskProvider<out Jar>,
        isHostSpecific: Boolean
    ): KotlinCompilation<*> {
        val project = target.project

        val compilationName = sourceSet.name
        val platformCompilations = sourceSet.internal.awaitPlatformCompilations()
        val isNativeSourceSet = sourceSet.isNativeSourceSet.await()

        val compilationFactory: KotlinCompilationFactory<out KotlinCompilation<*>> = when {
            isNativeSourceSet -> KotlinSharedNativeCompilationFactory(
                target = target,
                konanTargets = platformCompilations.map { (it as AbstractKotlinNativeCompilation).konanTarget }.toSet(),
                defaultSourceSet = sourceSet
            )

            else -> KotlinCommonCompilationFactory(
                target = target, defaultSourceSet = sourceSet
            )
        }

        return compilationFactory.create(compilationName).apply {
            target.compilations.add(this@apply)

            configureMetadataDependenciesForCompilation(this@apply)

            if (!isHostSpecific) {
                val metadataContent = project.filesWithUnpackedArchives(this@apply.output.allOutputs, setOf("klib"))
                allMetadataJar.configure { it.from(metadataContent) { spec -> spec.into(this@apply.defaultSourceSet.name) } }
                if (this is KotlinSharedNativeCompilation) {
                    project.includeCommonizedCInteropMetadata(allMetadataJar, this)
                }
            } else {
                if (platformCompilations.filterIsInstance<KotlinNativeCompilation>().none { it.konanTarget.enabledOnCurrentHost }) {
                    // Then we don't have any platform module to put this compiled source set to, so disable the compilation task:
                    compileKotlinTaskProvider.configure { it.enabled = false }
                    // Also clear the dependency files (classpath) of the compilation so that the host-specific dependencies are
                    // not resolved:
                    compileDependencyFiles = project.files()
                }
            }

            target.project.runOnceAfterEvaluated("Sync common compilation language settings to compiler options") {
                target.compilations.all { compilation ->
                    applyLanguageSettingsToCompilerOptions(
                        compilation.defaultSourceSet.languageSettings,
                        compilation.compilerOptions.options
                    )
                }
            }
        }
    }

    private suspend fun configureMetadataDependenciesForCompilation(compilation: KotlinCompilation<*>) {
        val project = compilation.target.project
        val sourceSet = compilation.defaultSourceSet

        val transformationTask = project.locateOrRegisterMetadataDependencyTransformationTask(sourceSet)

        val artifacts = sourceSet.internal.resolvableMetadataConfiguration.incoming.artifacts.getResolvedArtifactsCompat(project)

        // Metadata from visible source sets within dependsOn closure
        compilation.compileDependencyFiles += sourceSet.dependsOnClassesDirs

        // Requested dependencies that are not Multiplatform Libraries. for example stdlib-common
        compilation.compileDependencyFiles += project.files(artifacts.map { it.filterNot { it.isMpp }.map { it.file } })

        // Transformed Multiplatform Libraries based on source set visibility
        compilation.compileDependencyFiles += project.files(transformationTask.map { it.allTransformedLibraries })

        if (sourceSet is DefaultKotlinSourceSet && sourceSet.sharedCommonizerTarget.await() is SharedCommonizerTarget) {
            compilation.compileDependencyFiles += project.createCInteropMetadataDependencyClasspath(sourceSet)
        }
    }

    private val ResolvedArtifactResult.isMpp: Boolean get() = variant.attributes.containsMultiplatformAttributes

    private val KotlinSourceSet.dependsOnClassesDirs: FileCollection
        get() = project.filesProvider {
            internal.dependsOnClosure.mapNotNull { hierarchySourceSet ->
                val compilation = project.future { findMetadataCompilation(hierarchySourceSet) }.getOrThrow() ?: return@mapNotNull null
                compilation.output.classesDirs
            }
        }

    private fun createCommonMainElementsConfiguration(target: KotlinMetadataTarget) {
        val project = target.project
        project.configurations.create(COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME).apply {
            isCanBeConsumed = true
            isCanBeResolved = false
            usesPlatformOf(target)

            attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
            attributes.attribute(CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))

            val commonMainApiConfiguration = project.configurations.sourceSetDependencyConfigurationByScope(
                project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME),
                KotlinDependencyScope.API_SCOPE
            )
            extendsFrom(commonMainApiConfiguration)

            project.artifacts.add(name, project.tasks.getByName(target.legacyArtifactsTaskName))
        }
    }
}

internal class NativeSharedCompilationProcessor(
    private val compilation: KotlinSharedNativeCompilation
) : KotlinCompilationProcessor<KotlinNativeCompile>(KotlinCompilationInfo(compilation)) {

    override val kotlinTask: TaskProvider<out KotlinNativeCompile> =
        KotlinNativeTargetConfigurator.createKlibCompilationTask(compilationInfo, compilation.konanTarget)

    override fun run() = Unit
}

internal fun Project.createGenerateProjectStructureMetadataTask(module: GradleKpmModule): TaskProvider<GenerateProjectStructureMetadata> =
    project.registerTask(lowerCamelCaseName("generate", module.moduleClassifier, "ProjectStructureMetadata")) { task ->
        task.lazyKotlinProjectStructureMetadata = lazy { buildProjectStructureMetadata(module) }
        task.description = "Generates serialized project structure metadata of module '${module.name}' (for tooling)"
    }

internal fun Project.createGenerateProjectStructureMetadataTask(): TaskProvider<GenerateProjectStructureMetadata> =
    project.registerTask(lowerCamelCaseName("generateProjectStructureMetadata")) { task ->
        task.lazyKotlinProjectStructureMetadata = lazy { project.multiplatformExtension.kotlinProjectStructureMetadata }
        task.description = "Generates serialized project structure metadata of the current project (for tooling)"
    }

internal interface ResolvedMetadataFilesProvider {
    val buildDependencies: Iterable<TaskProvider<*>>
    val metadataResolutions: Iterable<MetadataDependencyResolution>
    val metadataFilesByResolution: Map<out MetadataDependencyResolution, FileCollection>
}


internal val KotlinSourceSet.isNativeSourceSet: Future<Boolean> by futureExtension("isNativeSourceSet") {
    val compilations = internal.awaitPlatformCompilations()
    compilations.isNotEmpty() && compilations.all { it.platformType == KotlinPlatformType.native }
}

internal fun isSinglePlatformTypeSourceSet(sourceSet: KotlinSourceSet): Boolean {
    val platformCompilations = sourceSet.internal.compilations.filterNot { it.platformType == KotlinPlatformType.common }
    return platformCompilations.map { it.platformType }.toSet().size == 1
}

internal fun isSingleKotlinTargetSourceSet(sourceSet: KotlinSourceSet): Boolean {
    val platformCompilations = sourceSet.internal.compilations.filterNot { it.platformType == KotlinPlatformType.common }
    return platformCompilations.map { it.target }.toSet().size == 1
}

internal fun dependsOnClosureWithInterCompilationDependencies(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> =
    sourceSet.internal.dependsOnClosure.toMutableSet().apply {
        addAll(getVisibleSourceSetsFromAssociateCompilations(sourceSet))

    }

/**
 * @return All common source sets that can potentially be published. Right now, not all combinations of platforms actually
 * support metadata compilation (see [KotlinMetadataTargetConfigurator.isMetadataCompilationSupported].
 * Those compilations will be created but the corresponding tasks will be disabled.
 */
internal suspend fun getCommonSourceSetsForMetadataCompilation(project: Project): Set<KotlinSourceSet> {
    if (!project.shouldCompileIntermediateSourceSetsToMetadata)
        return setOf(project.multiplatformExtension.awaitSourceSets().getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))

    val compilationsBySourceSet: Map<KotlinSourceSet, Set<KotlinCompilation<*>>> =
        project.kotlinExtension.awaitSourceSets().associateWith { it.internal.awaitPlatformCompilations() }

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

internal suspend fun getPublishedPlatformCompilations(project: Project): Map<KotlinUsageContext, KotlinCompilation<*>> {
    val result = mutableMapOf<KotlinUsageContext, KotlinCompilation<*>>()

    project.multiplatformExtension.awaitTargets().withType(AbstractKotlinTarget::class.java).forEach { target ->
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
            @Suppress("IMPLICIT_CAST_TO_ANY")
            if (it.extension in extensions) {
                if (it.exists()) project.zipTree(it) else null
            } else it
        }
    }).builtBy(from)

private val KotlinMetadataTarget.metadataCompilationsCreated: CompletableFuture<Unit> by extrasLazyProperty("metadataCompilationsCreated") {
    CompletableFuture()
}

internal suspend fun KotlinMetadataTarget.awaitMetadataCompilationsCreated(): NamedDomainObjectContainer<KotlinCompilation<*>> {
    metadataCompilationsCreated.await()
    return compilations
}

internal suspend fun Project.findMetadataCompilation(sourceSet: KotlinSourceSet): KotlinMetadataCompilation<*>? {
    val metadataTarget = multiplatformExtension.metadata() as KotlinMetadataTarget
    metadataTarget.awaitMetadataCompilationsCreated()
    return metadataTarget.compilations.findByName(sourceSet.name) as KotlinMetadataCompilation<*>?
}
