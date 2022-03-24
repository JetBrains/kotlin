/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.metadata

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil.compilationsBySourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.hasKpmModel
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.native.internal.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import java.util.concurrent.Callable

internal const val COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME = "commonMainMetadataElements"
internal const val ALL_COMPILE_METADATA_CONFIGURATION_NAME = "allSourceSetsCompileDependenciesMetadata"
internal const val ALL_RUNTIME_METADATA_CONFIGURATION_NAME = "allSourceSetsRuntimeDependenciesMetadata"

internal val Project.isKotlinGranularMetadataEnabled: Boolean
    get() = project.hasKpmModel || with(PropertiesProvider(rootProject)) {
        mppHierarchicalStructureByDefault || // then we want to use KLIB granular compilation & artifacts even if it's just commonMain
                hierarchicalStructureSupport ||
                enableGranularSourceSetsMetadata == true
    }

internal val Project.shouldCompileIntermediateSourceSetsToMetadata: Boolean
    get() = project.hasKpmModel || with(PropertiesProvider(rootProject)) {
        when {
            !hierarchicalStructureSupport && mppHierarchicalStructureByDefault -> false
            else -> true
        }
    }

internal val Project.isCompatibilityMetadataVariantEnabled: Boolean
    get() = PropertiesProvider(this).enableCompatibilityMetadataVariant == true

class KotlinMetadataTargetConfigurator :
    KotlinOnlyTargetConfigurator<AbstractKotlinCompilation<*>, KotlinMetadataTarget>(
        createDefaultSourceSets = false,
        createTestCompilation = false
    ) {
    companion object {
        internal const val ALL_METADATA_JAR_NAME = "allMetadataJar"

        internal fun transformGranularMetadataTaskName(sourceSetName: String) =
            lowerCamelCaseName("transform", sourceSetName, "DependenciesMetadata")
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
                attributes.attribute(CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))
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

        /** See [createMetadataDependencyTransformationClasspath] and its usage. */
        if (project.isKotlinGranularMetadataEnabled && compilation.name != KotlinCompilation.MAIN_COMPILATION_NAME)
            compilation.compileDependencyFiles = project.files()
        else
            super.setupCompilationDependencyFiles(compilation)
    }

    override fun buildCompilationProcessor(compilation: AbstractKotlinCompilation<*>): KotlinCompilationProcessor<*> = when (compilation) {
        is KotlinCommonCompilation -> {
            val tasksProvider = KotlinTasksProvider()
            KotlinCommonSourceSetProcessor(compilation, tasksProvider)
        }
        is KotlinSharedNativeCompilation -> NativeSharedCompilationProcessor(compilation)
        else -> error("unsupported compilation type ${compilation::class.qualifiedName}")
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
            it.from(generateMetadata.map { it.resultFile }) { spec ->
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
                sourceSet.withDependsOnClosure.forEach { hierarchySourceSet ->
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
                attributes.attribute(CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
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

        check(!project.hasKpmModel) { "KotlinMetadataTargetConfigurator cannot work with KPM!" }

        val compilationName = sourceSet.name

        val platformCompilations = compilationsBySourceSets(project)
            .getValue(sourceSet).filter { it.target.name != KotlinMultiplatformPlugin.METADATA_TARGET_NAME }

        val isNativeSourceSet = isSharedNativeSourceSet(project, sourceSet)

        val compilationFactory: KotlinCompilationFactory<out AbstractKotlinCompilation<*>> = when {
            isNativeSourceSet -> KotlinSharedNativeCompilationFactory(
                target,
                platformCompilations.map { (it as AbstractKotlinNativeCompilation).konanTarget }
            )
            else -> KotlinCommonCompilationFactory(target)
        }

        return compilationFactory.create(compilationName).apply {
            target.compilations.add(this@apply)

            (compilationDetails as DefaultCompilationDetails<*>).addExactSourceSetsEagerly(setOf(sourceSet))

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
        }
    }

    private fun configureMetadataDependenciesForCompilation(compilation: AbstractKotlinCompilation<*>) {
        val project = compilation.target.project
        val sourceSet = compilation.defaultSourceSet

        project.registerTask<TransformKotlinGranularMetadata>(
            transformGranularMetadataTaskName(compilation.name),
            listOf(sourceSet)
        ) { }

        compilation.compileDependencyFiles += createMetadataDependencyTransformationClasspath(
            project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME),
            compilation
        )

        if (compilation is KotlinSharedNativeCompilation && sourceSet is DefaultKotlinSourceSet) {
            compilation.compileDependencyFiles += project.createCInteropMetadataDependencyClasspath(sourceSet)
        }
    }

    private fun setupDependencyTransformationForSourceSet(
        project: Project,
        sourceSet: KotlinSourceSet,
        isSourceSetPublished: Boolean
    ) {
        KotlinDependencyScope.values().forEach { scope ->
            val granularMetadataTransformation = GranularMetadataTransformation(
                project,
                sourceSet,
                listOf(scope),
                lazy {
                    dependsOnClosureWithInterCompilationDependencies(project, sourceSet).filterIsInstance<DefaultKotlinSourceSet>()
                        .map { checkNotNull(it.dependencyTransformations[scope]) }
                }
            )

            if (sourceSet is DefaultKotlinSourceSet)
                sourceSet.dependencyTransformations[scope] = granularMetadataTransformation

            val sourceSetDependencyConfigurationByScope = project.sourceSetDependencyConfigurationByScope(sourceSet, scope)

            if (isSourceSetPublished) {
                if (scope != KotlinDependencyScope.COMPILE_ONLY_SCOPE) {
                    project.addExtendsFromRelation(
                        ALL_RUNTIME_METADATA_CONFIGURATION_NAME,
                        sourceSetDependencyConfigurationByScope.name
                    )
                }
                if (scope != KotlinDependencyScope.RUNTIME_ONLY_SCOPE) {
                    project.addExtendsFromRelation(
                        ALL_COMPILE_METADATA_CONFIGURATION_NAME,
                        sourceSetDependencyConfigurationByScope.name
                    )
                }
            }

            if (!PropertiesProvider(project).experimentalKpmModelMapping) {
                val sourceSetMetadataConfigurationByScope = project.sourceSetMetadataConfigurationByScope(sourceSet, scope)
                granularMetadataTransformation.applyToConfiguration(sourceSetMetadataConfigurationByScope)
                if (scope != KotlinDependencyScope.COMPILE_ONLY_SCOPE) {
                    project.addExtendsFromRelation(
                        sourceSetMetadataConfigurationByScope.name,
                        ALL_COMPILE_METADATA_CONFIGURATION_NAME
                    )
                }
                if (scope != KotlinDependencyScope.RUNTIME_ONLY_SCOPE) {
                    project.addExtendsFromRelation(
                        sourceSetMetadataConfigurationByScope.name,
                        ALL_COMPILE_METADATA_CONFIGURATION_NAME
                    )
                }
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

    private fun createMetadataDependencyTransformationClasspath(
        fromFiles: Configuration,
        compilation: AbstractKotlinCompilation<*>
    ): FileCollection {
        val project = compilation.target.project

        val sourceSet = compilation.defaultSourceSet

        val dependsOnCompilationOutputs = lazy {
            sourceSet.withDependsOnClosure.mapNotNull { hierarchySourceSet ->
                val dependencyCompilation = project.getMetadataCompilationForSourceSet(hierarchySourceSet)
                dependencyCompilation?.output?.classesDirs.takeIf { hierarchySourceSet != sourceSet }
            }
        }

        val resolvedMetadataFilesProviders = lazy {
            val transformationTaskHolders = sourceSet.withDependsOnClosure.mapNotNull { hierarchySourceSet ->
                project.locateTask<TransformKotlinGranularMetadata>(transformGranularMetadataTaskName(hierarchySourceSet.name))
            }
            transformationTaskHolders.map { SourceSetResolvedMetadataProvider(it) }
        }

        return createMetadataDependencyTransformationClasspath(
            project,
            fromFiles,
            dependsOnCompilationOutputs,
            resolvedMetadataFilesProviders
        )
    }

    private fun createCommonMainElementsConfiguration(target: KotlinMetadataTarget) {
        val project = target.project
        project.configurations.create(COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME).apply {
            isCanBeConsumed = true
            isCanBeResolved = false
            usesPlatformOf(target)

            attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
            attributes.attribute(CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))

            val commonMainApiConfiguration = project.sourceSetDependencyConfigurationByScope(
                project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME),
                KotlinDependencyScope.API_SCOPE
            )
            extendsFrom(commonMainApiConfiguration)

            project.artifacts.add(name, project.tasks.getByName(target.legacyArtifactsTaskName))
        }
    }
}

internal class NativeSharedCompilationProcessor(
    override val kotlinCompilation: KotlinNativeFragmentMetadataCompilationData
) : KotlinCompilationProcessor<KotlinNativeCompile>(kotlinCompilation) {

    override val kotlinTask: TaskProvider<out KotlinNativeCompile> =
        KotlinNativeTargetConfigurator.createKlibCompilationTask(kotlinCompilation)

    override fun run() = Unit
}

internal fun Project.createGenerateProjectStructureMetadataTask(module: KotlinGradleModule): TaskProvider<GenerateProjectStructureMetadata> =
    project.registerTask(lowerCamelCaseName("generate", module.moduleClassifier, "ProjectStructureMetadata")) { task ->
        task.lazyKotlinProjectStructureMetadata = lazy { buildProjectStructureMetadata(module) }
    }

internal fun Project.createGenerateProjectStructureMetadataTask(): TaskProvider<GenerateProjectStructureMetadata> =
    project.registerTask(lowerCamelCaseName("generateProjectStructureMetadata")) { task ->
        task.lazyKotlinProjectStructureMetadata = lazy { checkNotNull(buildKotlinProjectStructureMetadata(project)) }
    }

internal interface ResolvedMetadataFilesProvider {
    val buildDependencies: Iterable<TaskProvider<*>>
    val metadataResolutions: Iterable<MetadataDependencyResolution>
    val metadataFilesByResolution: Map<out MetadataDependencyResolution, FileCollection>
}

internal fun createMetadataDependencyTransformationClasspath(
    project: Project,
    fromFiles: Configuration,
    parentCompiledMetadataFiles: Lazy<Iterable<FileCollection>>,
    metadataResolutionProviders: Lazy<Iterable<ResolvedMetadataFilesProvider>>,
): FileCollection {
    return project.files(
        Callable {
            val allResolutionsByComponentId: Map<ComponentIdentifier, List<MetadataDependencyResolution>> =
                mutableMapOf<ComponentIdentifier, MutableList<MetadataDependencyResolution>>().apply {
                    metadataResolutionProviders.value.forEach {
                        it.metadataResolutions.forEach { resolution ->
                            getOrPut(resolution.dependency.id) { mutableListOf() }.add(resolution)
                        }
                    }
                }

            val transformedFilesByResolution: Map<MetadataDependencyResolution, FileCollection> =
                metadataResolutionProviders.value.flatMap { it.metadataFilesByResolution.toList() }.toMap()

            val artifactView = fromFiles.incoming.artifactView { view ->
                view.componentFilter { id ->
                    allResolutionsByComponentId[id].let { resolutions ->
                        resolutions == null || resolutions.any { it !is MetadataDependencyResolution.ExcludeAsUnrequested }
                    }
                }
            }

            mutableSetOf<Any /* File | FileCollection */>().apply {
                addAll(metadataResolutionProviders.value.map { project.files().builtBy(it.buildDependencies) })
                addAll(parentCompiledMetadataFiles.value)
                artifactView.artifacts.forEach { artifact ->
                    val resolutions = allResolutionsByComponentId[artifact.id.componentIdentifier]
                    if (resolutions == null) {
                        add(artifact.file)
                    } else {
                        val chooseVisibleSourceSets = resolutions.filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()

                        if (chooseVisibleSourceSets.isNotEmpty()) {
                            // Wrap the list into a FileCollection, as some older Gradle version failed to resolve the classpath
                            add(project.files(chooseVisibleSourceSets.map { transformedFilesByResolution.getValue(it) }))
                        } else if (resolutions.any { it is MetadataDependencyResolution.KeepOriginalDependency }) {
                            add(artifact.file)
                        } // else: all dependency transformations exclude this dependency as unrequested; don't add any files
                    }
                }
            }
        }
    )
}

internal fun isSharedNativeSourceSet(project: Project, sourceSet: KotlinSourceSet): Boolean {
    val compilations = compilationsBySourceSets(project)[sourceSet].orEmpty()
    return compilations.isNotEmpty() && compilations.all {
        it.platformType == KotlinPlatformType.common || it.platformType == KotlinPlatformType.native
    }
}

internal fun dependsOnClosureWithInterCompilationDependencies(project: Project, sourceSet: KotlinSourceSet): Set<KotlinSourceSet> =
    sourceSet.dependsOnClosure.toMutableSet().apply {
        addAll(getVisibleSourceSetsFromAssociateCompilations(project, sourceSet))
    }

/**
 * @return All common source sets that can potentially be published. Right now, not all combinations of platforms actually
 * support metadata compilation (see [KotlinMetadataTargetConfigurator.isMetadataCompilationSupported].
 * Those compilations will be created but the corresponding tasks will be disabled.
 */
internal fun getCommonSourceSetsForMetadataCompilation(project: Project): Set<KotlinSourceSet> {
    if (!project.shouldCompileIntermediateSourceSetsToMetadata)
        return setOf(project.multiplatformExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))

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
            @Suppress("IMPLICIT_CAST_TO_ANY")
            if (it.extension in extensions) {
                if (it.exists()) project.zipTree(it) else null
            } else it
        }
    }).builtBy(from)

internal fun Project.getMetadataCompilationForSourceSet(sourceSet: KotlinSourceSet): AbstractKotlinCompilation<*>? {
    return multiplatformExtension.metadata().compilations.findByName(sourceSet.name)
}
