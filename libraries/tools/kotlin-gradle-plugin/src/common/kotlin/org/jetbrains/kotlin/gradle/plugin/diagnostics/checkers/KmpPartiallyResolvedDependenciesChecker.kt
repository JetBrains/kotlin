package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation
import org.jetbrains.kotlin.gradle.plugin.mpp.KmpMultiVariantModuleIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetVisibilityProvider.PlatformCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.kmpMultiVariantModuleIdentifier
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationComponent
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import org.jetbrains.kotlin.gradle.utils.findAppliedAndroidPluginIdOrNull
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.isAllGradleProjectsEvaluated
import org.jetbrains.kotlin.gradle.utils.multiplatformAndroidLibraryPluginId
import java.util.concurrent.atomic.AtomicBoolean

@DisableCachingByDefault(because = "This task is not intended for execution")
internal abstract class KmpPartiallyResolvedDependenciesCheckerProjectsEvaluated : DefaultTask() {
    @TaskAction
    fun noop() {
        throw StopExecutionException()
    }

    companion object {
        const val TASK_NAME = "kmpPartiallyResolvedDependenciesChecker"
    }
}


internal object KmpPartiallyResolvedDependenciesChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (!project.isPartiallyResolvedDependenciesCheckerEnabled) return
        /**
         * If we are in a composite build, do all resolutions in a provider instead. This is not great because we can't guarantee that some
         * property will not try to serialize/compute task graph and fail with a resolution error.
         *
         * See https://github.com/gradle/gradle/issues/34200 and [oKmpPartiallyResolvedDependenciesCheckerIT.partially resolved kmp dependencies checker - smoke test included build]
         */
        val hasIncludedBuilds = project.gradle.includedBuilds.isNotEmpty()
        /**
         * Suppress this diagnostic during build scripts evaluation, and only run it during task graph construction. This prevents
         * cross-project configurations in coroutines and serialization from mutating finalized by resolution dependencies and other issues
         * like KT-79315.
         */
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()
        if (project.kotlinPropertiesProvider.eagerUnresolvedDependenciesDiagnostic) {
            val postProjectsEvaluationExecutionTask = project.locateOrRegisterPartiallyResolvedDependenciesCheckerTask()
            postProjectsEvaluationExecutionTask.configure { task ->
                if (!project.isAllGradleProjectsEvaluated) {
                    return@configure
                }
                val metadataTransformations = runCatching {
                    project.future {
                        project.multiplatformExtension.metadataTarget.publishedMetadataCompilations().map { compilation ->
                            GranularMetadataTransformation.Params(
                                project = project,
                                kotlinSourceSet = compilation.defaultSourceSet,
                            )
                        }
                    }.getOrThrow()
                }.getOrNull() ?: return@configure
                val validate = {
                    metadataTransformations.forEach { transformationParameters ->
                        project.validateNoTargetPlatformsResolvedPartially(
                            sourceSetName = transformationParameters.sourceSetName,
                            dependingPlatformCompilations = transformationParameters.dependingPlatformCompilations,
                            metadataConfiguration = transformationParameters.resolvedMetadataConfiguration,
                        )
                    }
                }
                val isMultiplatformAndroidLibraryPluginApplied = project.pluginManager.hasPlugin(multiplatformAndroidLibraryPluginId)
                val isAndroidPluginApplied = (project.findAppliedAndroidPluginIdOrNull() != null) || isMultiplatformAndroidLibraryPluginApplied
                /**
                 * AGP adds a checker that emits a diagnostic if a configuration is resolved before taskGraph.whenReady. Delay the check to
                 * execution in this case
                 */
                if (isAndroidPluginApplied || hasIncludedBuilds) {
                    val validationProvider = project.provider { validate() }
                    task.doFirst {
                        validationProvider.get()
                    }
                } else {
                    validate()
                }
            }
            project.tasks.withType<KotlinCompileTool>().configureEach {
                it.dependsOn(postProjectsEvaluationExecutionTask)
            }
        } else {
            project.tasks.withType<MetadataDependencyTransformationTask>().configureEach {
                if (!project.isAllGradleProjectsEvaluated) {
                    return@configureEach
                }
                val isAndroidPluginApplied = project.findAppliedAndroidPluginIdOrNull() != null
                if (isAndroidPluginApplied) {
                    return@configureEach
                }
                val validate = {
                    project.validateNoTargetPlatformsResolvedPartially(
                        sourceSetName = it.transformationParameters.sourceSetName,
                        dependingPlatformCompilations = it.transformationParameters.dependingPlatformCompilations,
                        metadataConfiguration = it.transformationParameters.resolvedMetadataConfiguration,
                    )
                }
                if (hasIncludedBuilds) {
                    it.dependsOn(
                        project.provider {
                            validate()
                            project.files()
                        }
                    )
                } else {
                    validate()
                }
            }
        }
    }
}

internal data class UnresolvedKmpDependency(
    var resolvedMetadataComponentIdentifier: ComponentIdentifier? = null,
    val resolvedVariants: MutableList<ResolvedVariant> = mutableListOf(),
    val unresolvedComponents: MutableList<UnresolvedComponent> = mutableListOf(),
) {
    data class ResolvedVariant(
        val targetName: String,
        val compilationName: String,
        val configurationName: String,
        val variant: String,
    )

    data class UnresolvedComponent(
        val targetName: String,
        val compilationName: String,
        val configurationName: String,
        val failureDescription: String,
    )
}

private fun Project.validateNoTargetPlatformsResolvedPartially(
    sourceSetName: String,
    dependingPlatformCompilations: List<PlatformCompilationData>,
    metadataConfiguration: LazyResolvedConfigurationWithArtifacts,
) {
    val partiallyUnresolvedDependencies = partiallyUnresolvedPlatformDependencies(
        dependingPlatformCompilations = dependingPlatformCompilations,
        metadataConfiguration = metadataConfiguration.resolvedComponent,
    )

    if (partiallyUnresolvedDependencies.isEmpty()) return

    project.reportDiagnostic(
        KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies(
            sourceSetName,
            partiallyUnresolvedDependencies,
        )
    )
}

/**
 * This diagnostic is intended for cases when the consumed KMP dependency is offering a subset of targets relative to the source set where
 * it was consumed. This diagnostic is only emitted when a KMP dependency is partially unresolved, meaning:
 * - Metadata configuration resolved
 * - One or more platforms resolved
 *
 * to avoid reporting it in the cases when nothing resolved (e.g. suitable repository is not defined)
 *
 * The matching is done similarly to visibility inference algorithm in [org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetVisibilityProvider]
 *
 * This diagnostic is implemented at configuration phase rather than in [org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetVisibilityProvider.getVisibleSourceSets]
 * to be able to emit before CC serialization/task graph resolution failure.
 *
 * Currently, only direct dependencies are checked because they are specified by the user, and it's unclear how useful this diagnostic is for
 * transitive dependencies. It's also difficult to implement this check for transitive dependencies because they will not be directly visible
 * as UnresolvedDependencyResult.
 */
internal fun partiallyUnresolvedPlatformDependencies(
    dependingPlatformCompilations: List<PlatformCompilationData>,
    metadataConfiguration: LazyResolvedConfigurationComponent,
): List<KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies.UnresolvedKmpDependency> {
    val unresolvedDependenciesMap:
            MutableMap<KmpMultiVariantModuleIdentifier, UnresolvedKmpDependency> = mutableMapOf()
    dependingPlatformCompilations.forEach { platformCompilation ->
        val directUnresolvedDependencies = platformCompilation.resolvedDependenciesConfiguration
            .root.dependencies.filterIsInstance<UnresolvedDependencyResult>()

        val visitedDependencies = mutableSetOf<KmpMultiVariantModuleIdentifier>()
        directUnresolvedDependencies.forEach { unresolvedDependency ->
            val kmpIdentifier = unresolvedDependency.attempted.kmpMultiVariantModuleIdentifier()
            if (visitedDependencies.add(kmpIdentifier)) {
                unresolvedDependenciesMap.getOrPut(
                    unresolvedDependency.attempted.kmpMultiVariantModuleIdentifier(),
                    { UnresolvedKmpDependency() }
                ).unresolvedComponents.add(
                    UnresolvedKmpDependency.UnresolvedComponent(
                        targetName = platformCompilation.targetName,
                        compilationName = platformCompilation.compilationName,
                        configurationName = platformCompilation.resolvedDependenciesConfiguration.configurationName,
                        failureDescription = unresolvedDependency.failure.message ?: unresolvedDependency.failure.toString(),
                    )
                )
            }
        }
    }

    if (unresolvedDependenciesMap.isEmpty()) return emptyList()

    fun onMatchingResolvedDependencyInUnresolvedDependencies(
        configuration: LazyResolvedConfigurationComponent,
        action: (UnresolvedKmpDependency, ResolvedDependencyResult) -> Unit,
    ) {
        val visitedDependencies = mutableSetOf<KmpMultiVariantModuleIdentifier>()
        configuration.allResolvedDependencies.forEach { resolvedDependency ->
            val selectedKmpIdentifier = resolvedDependency.selected.id.kmpMultiVariantModuleIdentifier()
            if (visitedDependencies.add(selectedKmpIdentifier)) {
                unresolvedDependenciesMap[selectedKmpIdentifier]?.let {
                    action(it, resolvedDependency)
                }
            }
            /**
             * Handle dependency substitution since in this case selected and attempted above are guaranteed to be different
             */
            val requestedKmpIdentifier = resolvedDependency.requested.kmpMultiVariantModuleIdentifier()
            if (visitedDependencies.add(requestedKmpIdentifier)) {
                unresolvedDependenciesMap[requestedKmpIdentifier]?.let {
                    action(it, resolvedDependency)
                }
            }
        }
    }

    onMatchingResolvedDependencyInUnresolvedDependencies(metadataConfiguration) { unresolvedKmpDependency, resolvedDependency ->
        unresolvedKmpDependency.resolvedMetadataComponentIdentifier = resolvedDependency.selected.id
    }

    dependingPlatformCompilations.forEach { platformCompilation ->
        onMatchingResolvedDependencyInUnresolvedDependencies(platformCompilation.resolvedDependenciesConfiguration) { unresolvedKmpDependency, resolvedDependency ->
            unresolvedKmpDependency.resolvedVariants.add(
                UnresolvedKmpDependency.ResolvedVariant(
                    targetName = platformCompilation.targetName,
                    compilationName = platformCompilation.compilationName,
                    configurationName = platformCompilation.resolvedDependenciesConfiguration.configurationName,
                    variant = resolvedDependency.resolvedVariant.displayName,
                )
            )
        }
    }

    return unresolvedDependenciesMap.filter {
        (it.value.resolvedMetadataComponentIdentifier != null) || it.value.resolvedVariants.isNotEmpty()
    }.map { unresolvedKmpDependency ->
        KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies.UnresolvedKmpDependency(
            displayName = unresolvedKmpDependency.key.displayCoordinate,
            resolvedMetadataComponentIdentifier = unresolvedKmpDependency.value.resolvedMetadataComponentIdentifier,
            unresolvedComponents = unresolvedKmpDependency.value.unresolvedComponents,
            resolvedVariants = unresolvedKmpDependency.value.resolvedVariants,
        )
    }
}

internal val Project.isPartiallyResolvedDependenciesCheckerEnabled: Boolean
    get() = (project.multiplatformExtensionOrNull != null) && project.kotlinPropertiesProvider.unresolvedDependenciesDiagnostic

internal fun Project.locateOrRegisterPartiallyResolvedDependenciesCheckerTask(): TaskProvider<KmpPartiallyResolvedDependenciesCheckerProjectsEvaluated> {
    return locateOrRegisterTask(KmpPartiallyResolvedDependenciesCheckerProjectsEvaluated.TASK_NAME)
}