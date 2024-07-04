/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication.KotlinAndroidTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.AggregateResourcesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.KotlinTargetResourcesResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.ResolveResourcesFromDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import javax.inject.Inject

abstract class KotlinTargetResourcesPublicationImpl @Inject constructor(
    private val project: Project,
) : KotlinTargetResourcesPublication {

    internal data class TargetResources(
        val resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceRoot),
        val relativeResourcePlacement: Provider<File>,
    )

    private val targetsThatSupportPublication = listOf(
        KotlinJsIrTarget::class,
        KotlinNativeTarget::class,
        KotlinJvmTarget::class,
        KotlinAndroidTarget::class,
    )

    private val targetsThatSupportResolution = listOf(
        KotlinJsIrTarget::class,
        KotlinNativeTarget::class,
    )

    private val targetToResourcesMap: MutableMap<KotlinTarget, TargetResources> = mutableMapOf()
    private val androidTargetAssetsMap: MutableMap<KotlinAndroidTarget, TargetResources> = mutableMapOf()

    private val targetResourcesSubscribers: MutableMap<KotlinTarget, MutableList<(TargetResources) -> (Unit)>> = mutableMapOf()
    private val androidTargetAssetsSubscribers: MutableMap<KotlinAndroidTarget, MutableList<(TargetResources) -> (Unit)>> = mutableMapOf()

    internal fun subscribeOnPublishResources(
        target: KotlinTarget,
        notify: (TargetResources) -> (Unit),
    ) {
        targetToResourcesMap[target]?.let(notify)
        targetResourcesSubscribers.getOrPut(target, { mutableListOf() }).add(notify)
    }

    internal fun subscribeOnAndroidPublishAssets(
        target: KotlinAndroidTarget,
        notify: (TargetResources) -> (Unit),
    ) {
        androidTargetAssetsMap[target]?.let(notify)
        androidTargetAssetsSubscribers.getOrPut(target, { mutableListOf() }).add(notify)
    }

    override fun canPublishResources(target: KotlinTarget): Boolean {
        if (targetsThatSupportPublication.none { it.isInstance(target) }) return false
        if (target is KotlinAndroidTarget) {
            return AndroidGradlePluginVersion.current >= KotlinAndroidTargetResourcesPublication.MIN_AGP_VERSION
        }
        return true
    }

    override fun publishResourcesAsKotlinComponent(
        target: KotlinTarget,
        resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceRoot),
        relativeResourcePlacement: Provider<File>,
    ) {
        if (!canPublishResources(target)) {
            target.project.reportDiagnostic(KotlinToolingDiagnostics.ResourceMayNotBePublishedForTarget(target.name))
            return
        }
        if (targetToResourcesMap[target] != null) {
            target.project.reportDiagnostic(KotlinToolingDiagnostics.ResourcePublishedMoreThanOncePerTarget(target.name))
            return
        }

        val resources = TargetResources(
            resourcePathForSourceSet = resourcePathForSourceSet,
            relativeResourcePlacement = relativeResourcePlacement,
        )
        targetToResourcesMap[target] = resources
        targetResourcesSubscribers[target].orEmpty().forEach { notify ->
            notify(resources)
        }
    }

    override fun publishInAndroidAssets(
        target: KotlinAndroidTarget,
        resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceRoot),
        relativeResourcePlacement: Provider<File>,
    ) {
        if (androidTargetAssetsMap[target] != null) {
            target.project.reportDiagnostic(KotlinToolingDiagnostics.AssetsPublishedMoreThanOncePerTarget())
        }
        val resources = TargetResources(
            resourcePathForSourceSet = resourcePathForSourceSet,
            relativeResourcePlacement = relativeResourcePlacement,
        )
        androidTargetAssetsMap[target] = resources
        androidTargetAssetsSubscribers[target].orEmpty().forEach { notify ->
            notify(resources)
        }
    }

    override fun canResolveResources(target: KotlinTarget): Boolean {
        return targetsThatSupportResolution.any { it.isInstance(target) }
    }

    override fun resolveResources(target: KotlinTarget): Provider<File> {
        validateTargetResourcesAreResolvable(target)
        validateGradleVersionIsCompatibleWithResolutionStrategy(target.name)

        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        return setupResourceResolvingForTarget(target, mainCompilation)
    }

    fun setupResourceResolvingForTarget(target: KotlinTarget, compilation: KotlinCompilation<*>): Provider<File> {
        val prefix = if (compilation.isMain()) "" else compilation.name.toLowerCase()
        val aggregateResourcesTaskName = lowerCamelCaseName(prefix, target.name, "AggregateResources")

        project.locateTask<AggregateResourcesTask>(aggregateResourcesTaskName)?.let {
            return it.flatMap { it.outputDirectory.asFile }
        }

        val resolveResourcesFromDependenciesTask = project.registerTask<ResolveResourcesFromDependenciesTask>(
            lowerCamelCaseName(prefix, target.name, "ResolveResourcesFromDependencies")
        )
        val aggregateResourcesTask = project.registerTask<AggregateResourcesTask>(aggregateResourcesTaskName) { aggregate ->
            aggregate.resourcesFromDependenciesDirectory.set(resolveResourcesFromDependenciesTask.flatMap { it.outputDirectory })
            aggregate.outputDirectory.set(
                project.layout.buildDirectory.dir(
                    "$MULTIPLATFORM_RESOURCES_DIRECTORY/${
                        dashSeparatedName(prefix, "aggregated-resources")
                    }/${target.targetName}"
                )
            )
        }

        project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
            resolveResourcesFromDependencies(
                prefix = prefix,
                compilation = compilation,
                resolveResourcesFromDependenciesTask = resolveResourcesFromDependenciesTask,
                targetName = target.targetName,
            )
            if (compilation.isMain()) {
                resolveResourcesFromSelf(
                    compilation = compilation,
                    target = target,
                    aggregateResourcesTask = aggregateResourcesTask,
                )
            }
        }

        return aggregateResourcesTask.flatMap { it.outputDirectory.asFile }
    }

    private fun resolveResourcesFromDependencies(
        prefix: String,
        compilation: KotlinCompilation<*>,
        resolveResourcesFromDependenciesTask: TaskProvider<ResolveResourcesFromDependenciesTask>,
        targetName: String,
    ) {
        resolveResourcesFromDependenciesTask.configure {
            it.filterResourcesByExtension.set(
                project.kotlinPropertiesProvider
                    .mppFilterResourcesByExtension
                    .map { explicitlyEnabled ->
                        // Always filter resources configuration because it resolves klibs for dependency graph inheritance
                        explicitlyEnabled || project.kotlinPropertiesProvider.mppResourcesResolutionStrategy == KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration
                    }
            )
            it.archivesFromDependencies.from(
                project.kotlinPropertiesProvider.mppResourcesResolutionStrategy.resourceArchives(compilation)
            )
            it.outputDirectory.set(
                project.layout.buildDirectory.dir(
                    "$MULTIPLATFORM_RESOURCES_DIRECTORY/${
                        dashSeparatedName(
                            prefix,
                            "resources-from-dependencies"
                        )
                    }/${targetName}"
                )
            )
        }
    }

    private fun resolveResourcesFromSelf(
        compilation: KotlinCompilation<*>,
        target: KotlinTarget,
        aggregateResourcesTask: TaskProvider<AggregateResourcesTask>,
    ) {
        subscribeOnPublishResources(target) { resources ->
            val copyResourcesTask = compilation.assembleHierarchicalResources(
                target.disambiguateName("ResolveSelfResources"),
                resources,
            )
            aggregateResourcesTask.configure { aggregate ->
                aggregate.resourcesFromSelfDirectory.set(copyResourcesTask)
            }
        }
    }

    private fun validateTargetResourcesAreResolvable(target: KotlinTarget) {
        if (!canResolveResources(target)) {
            target.project.reportDiagnostic(KotlinToolingDiagnostics.ResourceMayNotBeResolvedForTarget(target.name))
        }
    }

    private fun validateGradleVersionIsCompatibleWithResolutionStrategy(targetName: String) {
        if (project.kotlinPropertiesProvider.mppResourcesResolutionStrategy == KotlinTargetResourcesResolutionStrategy.VariantReselection) {
            if (project.gradleVersion < minimumGradleVersionForVariantReselection) {
                project.reportDiagnosticOncePerBuild(
                    KotlinToolingDiagnostics.ResourceMayNotBeResolvedWithGradleVersion(
                        targetName,
                        GradleVersion.current().toString(),
                        minimumGradleVersionForVariantReselection.toString(),
                    )
                )
            }
        }
    }

    internal companion object {
        const val MULTIPLATFORM_RESOURCES_DIRECTORY = "kotlin-multiplatform-resources"
        const val RESOURCES_CLASSIFIER = "kotlin_resources"
        const val RESOURCES_ZIP_EXTENSION = "${RESOURCES_CLASSIFIER}.zip"

        const val RESOURCES_PATH = "ResourcesPath"

        val minimumGradleVersionForVariantReselection = GradleVersion.version("7.6")
    }

}
