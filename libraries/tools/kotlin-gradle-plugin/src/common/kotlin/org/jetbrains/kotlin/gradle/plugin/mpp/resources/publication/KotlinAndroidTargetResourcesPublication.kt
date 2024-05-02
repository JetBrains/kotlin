/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.assembleHierarchicalResources
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.registerAssembleHierarchicalResourcesTaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfo
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.jetbrains.kotlin.gradle.utils.androidPluginIds
import org.jetbrains.kotlin.gradle.utils.findByType
import org.jetbrains.kotlin.gradle.utils.getOrPut
import javax.inject.Inject

private val Project.kotlinMultiplatformAndroidResourcesPublication: KotlinAndroidTargetResourcesPublication
    get() {
        return extraProperties.getOrPut("kotlinMultiplatformAndroidResourcesPublication") {
            KotlinAndroidTargetResourcesPublication()
        }
    }

internal val SetUpMultiplatformAndroidAssetsAndResourcesPublicationAction = KotlinProjectSetupAction {
    if (!kotlinPropertiesProvider.mppResourcesPublication) return@KotlinProjectSetupAction

    project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
        multiplatformExtension.awaitTargets()
            .filterIsInstance<KotlinAndroidTarget>()
            .forEach { target ->
                target.compilations.forEach { compilation ->
                    target.setUpMultiplatformResourcesAndAssets(compilation)
                }
            }
    }

    var isConfigured = false
    androidPluginIds.forEach { pluginId ->
        pluginManager.withPlugin(pluginId) {
            if (isConfigured) {
                return@withPlugin
            }
            isConfigured = true

            // Bundle assets and resources only if AGP is 7.3.0+ due to "addGeneratedSourceDirectory" API availability. And the resources
            // API is only supported in CMP since 7.3.1
            if (AndroidGradlePluginVersion.current >= KotlinAndroidTargetResourcesPublication.MIN_AGP_VERSION) {
                project.extensions.findByType<AndroidComponentsExtension<*, *, *>>()?.onVariants { newAgpVariant ->
                    kotlinMultiplatformAndroidResourcesPublication.setUpCopyAssetsTasksForNewAgpVariants(
                        project,
                        newAgpVariant,
                    )
                }
            }
        }
    }
}

internal class KotlinAndroidTargetResourcesPublication {

    @DisableCachingByDefault
    internal abstract class AssetsCopyForAGPTask : DefaultTask() {
        @get:Inject
        abstract val fileSystem: FileSystemOperations

        @get:SkipWhenEmpty
        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:InputDirectory
        abstract val inputDirectory: DirectoryProperty

        @get:OutputDirectory
        abstract val outputDirectory: DirectoryProperty

        @TaskAction
        fun copy() {
            fileSystem.copy {
                it.from(inputDirectory)
                it.into(outputDirectory)
            }
        }
    }

    private val copyTaskFromVariantName: MutableMap<String, TaskProvider<AssetsCopyForAGPTask>> = mutableMapOf()
    private val subscribers: MutableMap<String, MutableList<(TaskProvider<AssetsCopyForAGPTask>) -> (Unit)>> = mutableMapOf()

    internal fun setUpCopyAssetsTasksForNewAgpVariants(
        project: Project,
        newAgpVariant: Variant,
    ) {
        val variantName = newAgpVariant.name
        if (copyTaskFromVariantName[variantName] != null) {
            error("Setting up multiple copy assets tasks for variant $variantName")
        }
        // Prepare these copy tasks in advance because onVariants API has a specific lifecycle requirement and only set up the input for
        // this task if assets publication is requested
        val copyTask = project.registerTask<AssetsCopyForAGPTask>("${variantName}AssetsCopyForAGP") {
            // Specify output just in case
            it.outputDirectory.convention(
                project.layout.buildDirectory.dir(
                    "${KotlinTargetResourcesPublicationImpl.MULTIPLATFORM_RESOURCES_DIRECTORY}/assemble-hierarchically/${variantName}-tmp-for-agp"
                )
            )
        }

        newAgpVariant.sources.assets?.addGeneratedSourceDirectory(
            taskProvider = copyTask,
            wiredWith = { task -> task.outputDirectory }
        )
        copyTaskFromVariantName[variantName] = copyTask
        subscribers[variantName].orEmpty().forEach { notify ->
            notify(copyTask)
        }
    }

    internal fun subscribeOnCopyAssetsTaskForVariant(
        variantName: String,
        notify: (TaskProvider<AssetsCopyForAGPTask>) -> (Unit),
    ) {
        // Associate between BaseVariant and Variant by name
        copyTaskFromVariantName[variantName]?.let(notify)
        subscribers.getOrPut(variantName, { mutableListOf() }).add(notify)
    }

    companion object {
        const val MIN_AGP_VERSION = "7.3.1"
    }

}


internal fun KotlinAndroidTarget.setUpMultiplatformResourcesAndAssets(
    compilation: KotlinJvmAndroidCompilation,
) {
    if (project.multiplatformExtensionOrNull == null) return

    val legacyAgpVariant = compilation.androidVariant
    val legacyAgpVariantName = getVariantName(legacyAgpVariant)
    project.multiplatformExtension.resourcesPublicationExtension?.subscribeOnPublishResources(this) { resources ->
        val copyResourcesTask = compilation.registerAssembleHierarchicalResourcesTaskProvider(
            disambiguateName("${legacyAgpVariantName}Resources"),
            resources,
        )

        legacyAgpVariant.processJavaResourcesProvider.configure { task ->
            task.from(copyResourcesTask.flatMap { it.outputDirectory })
        }

        project.androidExtension.sourceSets.getByName(
            compilation.defaultSourceSet.androidSourceSetInfo.androidSourceSetName
        ).resources.srcDir(
            copyResourcesTask.flatMap {
                it.outputDirectory.locationOnly
            }
        )
    }

    project.multiplatformExtension.resourcesPublicationExtension?.subscribeOnAndroidPublishAssets(this) { assets ->
        project.kotlinMultiplatformAndroidResourcesPublication.subscribeOnCopyAssetsTaskForVariant(legacyAgpVariantName) { copyTaskForAgp ->
            val copyAssetsTask = compilation.assembleHierarchicalResources(
                disambiguateName("${legacyAgpVariantName}Assets"),
                assets,
            )
            copyTaskForAgp.configure {
                it.inputDirectory.set(copyAssetsTask)
            }
        }
    }
}