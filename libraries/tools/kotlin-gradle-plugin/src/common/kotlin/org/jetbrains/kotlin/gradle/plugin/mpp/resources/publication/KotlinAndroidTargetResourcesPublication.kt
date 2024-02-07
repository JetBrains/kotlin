/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.isAtLeast
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.registerAssembleHierarchicalResourcesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfo
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.jetbrains.kotlin.gradle.utils.androidPluginIds
import org.jetbrains.kotlin.gradle.utils.findByType
import org.jetbrains.kotlin.gradle.utils.getOrPut
import javax.inject.Inject

// FIXME: Move this somewhere?
internal const val kotlinMultiplatformAndroidResourcesPublicationName = "kotlinMultiplatformAndroidResourcesPublication"

internal val Project.kotlinMultiplatformAndroidResourcesPublication: KotlinAndroidTargetResourcesPublication get() {
    return extraProperties.getOrPut(kotlinMultiplatformAndroidResourcesPublicationName) {
        KotlinAndroidTargetResourcesPublication()
    }
}

internal val SetUpMultiplatformAndroidAssetsPublicationAction = KotlinProjectSetupAction {
    // FIXME: Deduplicate with KotlinAndroidPlugin
    var isConfigured = false
    androidPluginIds.forEach { pluginId ->
        pluginManager.withPlugin(pluginId) {
            if (isConfigured) { return@withPlugin }
            isConfigured = true

            // FIXME: Execute this only if AGP is 7.3.0+ due to "addGeneratedSourceDirectory"?
            if (AndroidGradlePluginVersion.currentOrNull.isAtLeast("7.3.0")) {
                project.extensions.findByType<AndroidComponentsExtension<*, *, *>>()?.onVariants { newAgpVariant ->
                    kotlinMultiplatformAndroidResourcesPublication.setUpCopyTasksForNewAgpVariants(
                        project,
                        newAgpVariant,
                    )
                }
            }
        }
    }
}

internal class KotlinAndroidTargetResourcesPublication {

    internal abstract class AssetsCopyForAGPTask : DefaultTask() {

        @get:Inject
        abstract val fs: FileSystemOperations

        @get:SkipWhenEmpty
        @get:InputDirectory
        abstract val inputDirectory: DirectoryProperty

        @get:OutputDirectory
        abstract val outputDirectory: DirectoryProperty

        @TaskAction
        fun copy() {
            fs.copy {
                it.from(inputDirectory)
                it.into(outputDirectory)
            }
        }

    }

    private val copyTaskFromVariantName: MutableMap<String, TaskProvider<AssetsCopyForAGPTask>> = mutableMapOf()
    private val subscribers: MutableMap<String, MutableList<(TaskProvider<AssetsCopyForAGPTask>) -> (Unit)>> = mutableMapOf()

    internal fun setUpCopyTasksForNewAgpVariants(
        project: Project,
        newAgpVariant: Variant,
    ) {
        val variantName = newAgpVariant.name
        if (copyTaskFromVariantName[variantName] != null) {
            error("Setting up multiple copy tasks for variant ${variantName}")
        }
        // Prepare these copy tasks in advance because it is not possible to con
        val copyTask = project.registerTask<AssetsCopyForAGPTask>("${variantName}AssetsCopyForAGP") {
            // FIXME: Specify output just in case?
            it.outputDirectory.set(
                project.layout.buildDirectory.dir(
                    "${KotlinTargetResourcesPublication.MULTIPLATFORM_RESOURCES_DIRECTORY}/assemble-hierarchically/${variantName}-tmp-for-agp"
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

    internal fun subscribeOnCopyTaskForVariant(
        variantName: String,
        notify: (TaskProvider<AssetsCopyForAGPTask>) -> (Unit),
    ) {
        copyTaskFromVariantName[variantName]?.let(notify)
        subscribers.getOrPut(variantName, { mutableListOf() }).add(notify)
    }

}


internal suspend fun KotlinAndroidTarget.setUpResourcesAndAssetsPublication(
    compilation: KotlinCompilation<*>,
    variant: String,
) {
    project.multiplatformExtension.resourcesPublicationExtension.subscribeOnPublishResources(this) { resources ->
        project.launch {
            val copyResourcesTask = compilation.registerAssembleHierarchicalResourcesTask(
                disambiguateName("${variant}Resources"),
                resources,
            )
            project.androidExtension.sourceSets.getByName(
                compilation.defaultSourceSet.androidSourceSetInfo.androidSourceSetName
            ).resources.srcDir(copyResourcesTask)
        }
    }

    project.multiplatformExtension.resourcesPublicationExtension.subscribeOnAndroidPublishAssets(this) { assets ->
        project.kotlinMultiplatformAndroidResourcesPublication.subscribeOnCopyTaskForVariant(variant) { copyTaskForAgp ->
            project.launch {
                val assetsVariantName = disambiguateName("${variant}Assets")
                val copyAssetsTask = compilation.registerAssembleHierarchicalResourcesTask(
                    assetsVariantName,
                    assets,
                )
                copyTaskForAgp.configure {
                    it.inputDirectory.set(copyAssetsTask)
                }
            }
        }
    }
}