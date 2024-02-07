/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.AggregateResourcesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.ResolveResourcesFromDependenciesTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.io.File
import javax.inject.Inject

@InternalKotlinGradlePluginApi
interface KotlinTargetWithPublishableMultiplatformResources

// FIXME: Resolvable without publishable?
@InternalKotlinGradlePluginApi
interface KotlinTargetWithResolvableMultiplatformResources : KotlinTargetWithPublishableMultiplatformResources

interface KotlinTargetResourcesPublication {
    data class ResourceDescriptor(
        val absolutePath: Provider<File>,
        val includes: List<String>,
        val excludes: List<String>,
    )

    fun publishResourcesAsKotlinComponent(
        resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceDescriptor),
        relativeResourcePlacement: Provider<File>,
    )
}

interface KotlinAndroidTargetResourcesPublication {
    fun publishInAndroidAssets(
        resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceDescriptor),
        relativeResourcePlacement: Provider<File>,
    )
}

interface KotlinTargetResourcesResolution {
    fun resolveResources(): Provider<File>
}

// FIXME: Drop generic parameters or use an interface everywhere?
@InternalKotlinGradlePluginApi
internal abstract class KotlinTargetResourcesPublicationImpl<in T> @Inject constructor(
    target: T,
) : KotlinTargetResourcesPublication where T : KotlinTargetWithPublishableMultiplatformResources, T : KotlinTarget {
    private val project = target.project

    internal data class TargetResources(
        val resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceDescriptor),
        val relativeResourcePlacement: Provider<File>,
    )

    private val targetResourcesSubscribers: MutableList<(TargetResources) -> (Unit)> = mutableListOf()
    private var targetResources: TargetResources? = null

    internal fun subscribeOnPublishResources(notify: (TargetResources) -> (Unit)) {
        targetResources?.let(notify)
        targetResourcesSubscribers.add(notify)
    }

    override fun publishResourcesAsKotlinComponent(
        resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceDescriptor),
        // FIXME: Maybe Provider<String>
        relativeResourcePlacement: Provider<File>, // embed/$group.$name
    ) {
        if (targetResources != null) {
            error("Only one publication per target is allowed")
        }
        val resources = TargetResources(
            resourcePathForSourceSet = resourcePathForSourceSet,
            relativeResourcePlacement = relativeResourcePlacement,
        )
        targetResources = resources
        targetResourcesSubscribers.forEach { notify ->
            notify(resources)
        }
    }

    internal companion object {
        const val EXTENSION_NAME = "resourcesPublication"
        const val MULTIPLATFORM_RESOURCES_DIRECTORY = "kotlin-multiplatform-resources"
    }

}

@InternalKotlinGradlePluginApi
internal abstract class AndroidKotlinTargetResourcesPublicationImpl @Inject constructor(
    target: KotlinAndroidTarget,
) : KotlinAndroidTargetResourcesPublication, KotlinTargetResourcesPublicationImpl<KotlinAndroidTarget>(target) {

    private val androidTargetAssetsSubscribers: MutableList<(TargetResources) -> (Unit)> = mutableListOf()
    private var assets: TargetResources? = null

    internal fun subscribeOnAndroidPublishAssets(notify: (TargetResources) -> (Unit)) {
        assets?.let(notify)
        androidTargetAssetsSubscribers.add(notify)
    }

    override fun publishInAndroidAssets(
        resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceDescriptor),
        relativeResourcePlacement: Provider<File>, // embed/$group.$name
    ) {
        if (assets != null) {
            error("Only one assets publication per android target is allowed")
        }
        val resources = TargetResources(
            resourcePathForSourceSet = resourcePathForSourceSet,
            relativeResourcePlacement = relativeResourcePlacement,
        )
        androidTargetAssetsSubscribers.forEach { notify ->
            notify(resources)
        }
    }
}

@InternalKotlinGradlePluginApi
internal abstract class KotlinTargetResourcesResolutionImpl<in T> @Inject constructor(
    private val target: T,
) : KotlinTargetResourcesResolution where T : KotlinTargetWithResolvableMultiplatformResources, T : KotlinTargetWithPublishableMultiplatformResources, T : KotlinTarget {
    private val project = target.project

    override fun resolveResources(): Provider<File> {
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
                project.layout.buildDirectory.dir("${KotlinTargetResourcesPublicationImpl.MULTIPLATFORM_RESOURCES_DIRECTORY}/aggregated-resources/${target.targetName}")
            )
        }

        project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
            val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            val resourcesConfiguration = mainCompilation.internal.configurations.resourcesConfiguration
            resolveResourcesFromDependencies.configure {
                it.dependsOn(resourcesConfiguration)
                it.archivesFromDependencies.from(resourcesConfiguration.incoming.artifactView { view -> view.lenient(true) }.files)
                it.outputDirectory.set(
                    project.layout.buildDirectory.dir("${KotlinTargetResourcesPublicationImpl.MULTIPLATFORM_RESOURCES_DIRECTORY}/resources-from-dependencies/${target.targetName}")
                )
            }

            // FIXME: This is temporally coupled for Android target is a terrible way
            target.resourcesPublicationExtension.subscribeOnPublishResources { resources ->
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
        const val EXTENSION_NAME = "resourcesResolution"
    }
}