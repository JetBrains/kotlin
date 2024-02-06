/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.AggregateResourcesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.ResolveResourcesFromDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.io.File
import javax.inject.Inject

@InternalKotlinGradlePluginApi
abstract class KotlinTargetResourcesPublication
@Inject constructor(
    val project: Project,
) {

    // FIXME: Проверить какие таргеты создаются в presets
    private val targetsThatSupportPublication = listOf(
        KotlinJsIrTarget::class,
        KotlinNativeTarget::class,
        KotlinJvmTarget::class,
        // FIXME: С Андроидом могуть быть проблемы из-за того, что там создаются разные compilation для разных variant
        KotlinAndroidTarget::class,
    )

    private val targetsThatSupportResolution = listOf(
        KotlinJsIrTarget::class,
        KotlinNativeTarget::class,
    )

    data class ResourceDescriptor(
        val absolutePath: Provider<File>,
        val includes: List<String>,
        val excludes: List<String>,
    )

    data class TargetResources(
        val resourcePathForSourceSet: (KotlinSourceSet) -> (ResourceDescriptor),
        val relativeResourcePlacement: Provider<File>,
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

    fun publishResourcesAsKotlinComponent(
        target: KotlinTarget,
        resourcePathForSourceSet: (KotlinSourceSet) -> (ResourceDescriptor),
        // FIXME: Maybe Provider<String>
        relativeResourcePlacement: Provider<File>, // embed/$group.$name
    ) {
        if (!targetsThatSupportPublication.any { it.isInstance(target) }) {
            return
//            error("Resources may not be published for target $target")
        }
        if (targetToResourcesMap[target] != null) {
            error("Only one publication per target is allowed")
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

//    fun publishResourcesAsKotlinComponent(
//        target: KotlinTarget,
//        adjacentToDefaultSourceSetFolder: String,
//        relativeResourcePlacement: Provider<File>,
//    ) {
//        publishResourcesAsKotlinComponent(
//            target = target,
//            resourcePathForSourceSet = { sourceSet ->
//                ResourceDescriptor(
//                    project.provider { KotlinSourceSetFactory.defaultSourceFolder(project, sourceSet.name, adjacentToDefaultSourceSetFolder) },
//                    emptyList(),
//                    emptyList(),
//                )
//            },
//            relativeResourcePlacement = relativeResourcePlacement,
//        )
//    }

    fun publishInAndroidAssets(
        target: KotlinAndroidTarget,
        resourcePathForSourceSet: (KotlinSourceSet) -> (ResourceDescriptor),
        relativeResourcePlacement: Provider<File>, // embed/$group.$name
    ) {
        if (androidTargetAssetsMap[target] != null) {
            error("Only one assets publication per android target is allowed")
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

    fun resolveResources(target: KotlinTarget): Provider<File> {
        if (!targetsThatSupportResolution.any { it.isInstance(target) }) {
            error("Resources may not be resolved for target $target")
        }

        val aggregateResourcesTaskName = target.disambiguateName("AggregateResources")
        project.locateTask<AggregateResourcesTask>(aggregateResourcesTaskName)?.let {
            return it.flatMap { it.outputDirectory.asFile }
        }

        val resolveResourcesFromDependencies = project.registerTask<ResolveResourcesFromDependenciesTask>(
            target.disambiguateName("ResolveResourcesFromDependencies")
        )
        val aggregateResourcesTask = project.registerTask<AggregateResourcesTask>(aggregateResourcesTaskName) { aggregate ->
            aggregate.resourcesFromDependenciesDirectory.set(resolveResourcesFromDependencies.flatMap { it.outputDirectory })
            aggregate.outputDirectory.set(
                project.layout.buildDirectory.dir("$MULTIPLATFORM_RESOURCES_DIRECTORY/aggregated-resources/${target.targetName}")
            )
        }

        project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
            val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            val resourcesConfiguration = mainCompilation.internal.configurations.resourcesConfiguration
            resolveResourcesFromDependencies.configure {
                it.dependsOn(resourcesConfiguration)
                it.archivesFromDependencies.from(resourcesConfiguration.incoming.artifactView { view -> view.lenient(true) }.files)
                it.outputDirectory.set(
                    project.layout.buildDirectory.dir("$MULTIPLATFORM_RESOURCES_DIRECTORY/resources-from-dependencies/${target.targetName}")
                )
            }

            project.multiplatformExtension.resourcesPublicationExtension.subscribeOnPublishResources(
                target
            ) { resources ->
                project.launch {
                    val copyResourcesTask = mainCompilation.registerAssembleHierarchicalResourcesTask(
                        target.disambiguateName("ResolveSelfResources"),
                        resources,
                    )
                    aggregateResourcesTask.configure { aggregate ->
                        aggregate.resourcesFromSelfDirectory.set(copyResourcesTask)
                    }
                }
            }
        }

        return aggregateResourcesTask.flatMap { it.outputDirectory.asFile }
    }

    internal companion object {
        const val EXTENSION_NAME = "resourcesPublication"
        const val MULTIPLATFORM_RESOURCES_DIRECTORY = "kotlin-multiplatform-resources"
    }

}