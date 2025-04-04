/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.metadata

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.isFromUklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinMetadataConfigurationMetrics
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropMetadataDependencyClasspath
import org.jetbrains.kotlin.gradle.targets.native.internal.sharedCommonizerTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

/**
 * Metadata jar's classifier lived through the following iterations:
 * - "-all" when HMPP was introduced
 * - no classifier when per-HMPP metadata jar was dropped
 * - "-psm" with Uklibs because we want to move JVM jar into root publication and make in declassified for Maven JVM consumption
 */
internal val Project.psmJarClassifier: String?
    get() = when (kotlinPropertiesProvider.kmpPublicationStrategy) {
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> "psm"
        KmpPublicationStrategy.StandardKMPPublication -> null
    }

class KotlinMetadataTargetConfigurator :
    KotlinOnlyTargetConfigurator<KotlinCompilation<Any>, KotlinMetadataTarget>(createTestCompilation = false) {
    companion object {
        internal const val ALL_METADATA_JAR_NAME = "allMetadataJar"
    }

    override fun configureTarget(target: KotlinMetadataTarget) {
        super.configureTarget(target)

        target.project.addConfigurationMetrics {
            KotlinMetadataConfigurationMetrics.collectMetrics(it)
        }

        target.compilations.withType(KotlinCommonCompilation::class.java).getByName(KotlinCompilation.MAIN_COMPILATION_NAME).run {
            // Force the default 'main' compilation to produce *.kotlin_metadata regardless of the klib feature flag.
            forceCompilationToKotlinMetadata = true

            // Clear the dependencies of the compilation so that they don't take time resolving during task graph construction:
            compileDependencyFiles = target.project.files()
            compileTaskProvider.configure { it.onlyIf { false } }
        }

        createMetadataCompilationsForCommonSourceSets(target)

        configureMetadataDependenciesConfigurationsForCommonSourceSets(target)
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
    ) = target.project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseDsl) {
        withRestrictedStages(KotlinPluginLifecycle.Stage.upTo(KotlinPluginLifecycle.Stage.FinaliseCompilations)) {
            // Do this after all targets are configured by the user build script

            val publishedCommonSourceSets: Set<KotlinSourceSet> = getCommonSourceSetsForMetadataCompilation(project)
            val hostSpecificSourceSets: Set<KotlinSourceSet> = getHostSpecificSourceSets(project).toSet()

            val sourceSetsWithMetadataCompilations: Map<KotlinSourceSet, KotlinCompilation<*>> = publishedCommonSourceSets
                .associateWith { sourceSet ->
                    createMetadataCompilation(target, sourceSet, sourceSet in hostSpecificSourceSets)
                }
                .onEach { (sourceSet, compilation) ->
                    if (!isMetadataCompilationSupported(sourceSet)) {
                        compilation.compileTaskProvider.configure { it.enabled = false }
                    }
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

    private fun exportDependenciesForPublishing(
        compilation: KotlinCompilation<*>,
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
        isHostSpecific: Boolean,
    ): KotlinCompilation<*> {
        val project = target.project

        val compilationName = sourceSet.name
        val platformCompilations = sourceSet.internal.awaitPlatformCompilations()
        val isNativeSourceSet = sourceSet.isNativeSourceSet.await()

        val compilationFactory: KotlinCompilationFactory<out KotlinCompilation<Any>> = when {
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

            if (isHostSpecific) {
                // This logic can be simplified, see KT-64523
                val shouldBeDisabled = platformCompilations
                    .filterIsInstance<KotlinNativeCompilation>()
                    .none { it.target.enabledOnCurrentHostForKlibCompilation }
                if (shouldBeDisabled) {
                    // Then we don't have any platform module to put this compiled source set to, so disable the compilation task:
                    compileTaskProvider.configure { it.enabled = false }
                    // Also clear the dependency files (classpath) of the compilation so that the host-specific dependencies are
                    // not resolved:
                    compileDependencyFiles = project.files()
                }
            }
        }
    }

    private suspend fun configureMetadataDependenciesForCompilation(compilation: KotlinCompilation<*>) {
        val project = compilation.target.project
        val sourceSet = compilation.defaultSourceSet

        val transformationTask = project.locateOrRegisterMetadataDependencyTransformationTask(sourceSet)

        compilation.compileDependencyFiles = project.files()

        // Metadata from visible source sets within dependsOn closure
        compilation.compileDependencyFiles += sourceSet.dependsOnClosureCompilePath

        // Requested dependencies that are not Multiplatform Libraries. for example stdlib-common
        val artifacts = sourceSet.internal.resolvableMetadataConfiguration.incoming.artifacts.resolvedArtifacts
        compilation.compileDependencyFiles += project.files(artifacts.map { it.filterNot { it.isMpp }.map { it.file } })

        // Transformed Multiplatform Libraries based on source set visibility
        compilation.compileDependencyFiles += project.files(transformationTask.map { it.allTransformedLibraries() })

        if (sourceSet is DefaultKotlinSourceSet && sourceSet.sharedCommonizerTarget.await() is SharedCommonizerTarget) {
            compilation.compileDependencyFiles += project.createCInteropMetadataDependencyClasspath(sourceSet)
        }
    }

    private val ResolvedArtifactResult.isMpp: Boolean
        get() = variant.attributes.containsMultiplatformAttributes || isFromUklib
}

internal fun Project.locateOrRegisterGenerateProjectStructureMetadataTask(): TaskProvider<GenerateProjectStructureMetadata> =
    project.locateOrRegisterTask(lowerCamelCaseName("generateProjectStructureMetadata")) { task ->
        task.lazyKotlinProjectStructureMetadata = lazy { project.multiplatformExtension.kotlinProjectStructureMetadata }
        if (project.kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled) {
            task.addMetadataSourceSetsToOutput(project)
        }
        task.description = "Generates serialized project structure metadata of the current project (for tooling)"
    }

internal val KotlinSourceSet.isNativeSourceSet: Future<Boolean> by extrasStoredFuture {
    val compilations = internal.awaitPlatformCompilations()
    compilations.isNotEmpty() && compilations.all { it.platformType == KotlinPlatformType.native }
}

internal fun isSinglePlatformTypeSourceSet(sourceSet: KotlinSourceSet): Boolean =
    sourceSet.platformCompilations().map { it.platformType }.toSet().size == 1

internal fun isSingleKotlinTargetSourceSet(sourceSet: KotlinSourceSet): Boolean =
    sourceSet.platformCompilations().map { it.target }.toSet().size == 1

internal fun isMultipleKotlinTargetSourceSet(sourceSet: KotlinSourceSet): Boolean =
    sourceSet.platformCompilations().map { it.target }.toSet().size > 1

private fun KotlinSourceSet.platformCompilations() = internal.compilations.filterNot { it.platformType == KotlinPlatformType.common }

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

    project.multiplatformExtension.awaitTargets().withType(InternalKotlinTarget::class.java).forEach { target ->
        if (target.platformType == KotlinPlatformType.common)
            return@forEach

        target.kotlinComponents
            .flatMap { component -> component.internal.usages }
            .filter { it.includeIntoProjectStructureMetadata }
            .forEach { usage -> result[usage] = usage.compilation }
    }

    return result
}

private val KotlinMetadataTarget.metadataCompilationsCreated: CompletableFuture<Unit> by extrasLazyProperty("metadataCompilationsCreated") {
    CompletableFuture()
}

internal suspend fun KotlinMetadataTarget.awaitMetadataCompilationsCreated(): NamedDomainObjectContainer<KotlinCompilation<Any>> {
    metadataCompilationsCreated.await()
    return compilations
}

internal suspend fun Project.findMetadataCompilation(sourceSet: KotlinSourceSet): KotlinMetadataCompilation<*>? {
    val metadataTarget = multiplatformExtension.metadataTarget
    metadataTarget.awaitMetadataCompilationsCreated()
    return metadataTarget.compilations.findByName(sourceSet.name) as KotlinMetadataCompilation<*>?
}


/**
 * Contains all 'klibs' produced by compiling 'dependsOn' SourceSet's metadata.
 * The compile path can be passed to another metadata compilation as list of dependencies.
 *
 * Note: The compile path is ordered and will provide klibs containing corresponding actuals before providing
 * the klibs defining expects. This ordering is necessary for K2 as the compiler will not implement
 * its own 'actual over expect' discrimination anymore. K2 will use the first matching symbol of a given compile path.
 *
 * e.g.
 * When compiling a 'iosMain' source set, using the default hierarchy, we expect the order of the compile path:
 * ```
 * appleMain.klib, nativeMain.klib, commonMain.klib
 * ```
 *
 * Further details: https://youtrack.jetbrains.com/issue/KT-61540
 *
 */
internal val KotlinSourceSet.dependsOnClosureCompilePath: FileCollection
    get() = project.filesProvider {
        val topologicallySortedDependsOnClosure = internal.dependsOnClosure.sortedWith(Comparator { a, b ->
            when {
                a in b.internal.dependsOnClosure -> 1
                b in a.internal.dependsOnClosure -> -1
                /*
                SourceSet 'a' and SourceSet 'b' are not refining on each other,
                therefore no re-ordering is necessary (no requirements in this case).

                The original order of the 'dependsOnClosure' will be preserved, which will depend
                on the order of 'KotlinSourceSet.dependsOn' calls
                 */
                else -> 0
            }
        })

        topologicallySortedDependsOnClosure.mapNotNull { hierarchySourceSet ->
            val compilation = project.future { findMetadataCompilation(hierarchySourceSet) }.getOrThrow() ?: return@mapNotNull null
            compilation.output.classesDirs
        }
    }
