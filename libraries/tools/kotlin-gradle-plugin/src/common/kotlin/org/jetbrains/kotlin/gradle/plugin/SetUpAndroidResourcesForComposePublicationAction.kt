/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.utils.androidExtension


internal val SetUpAndroidResourcesForComposePublicationAction = KotlinProjectSetupAction {
    // FIXME: Launch in stage is needed to walk the source set graph. Can the source set graph walking logic suspend instead?
    launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
        multiplatformExtension.targets.filterIsInstance<KotlinAndroidTarget>().forEach { target ->
            target.publishLibraryVariants.orEmpty().forEach { variant ->
                setUpPublication(target, variant)
            }
        }
    }
}

private fun setUpPublication(target: KotlinAndroidTarget, variant: String) {
    // FIXME: Is there always a compilation per variant?
    val compilation = target.compilations.getByName(variant)
    val project = target.project
    val taskIdentifier = "${target.name.capitalize()}${variant.capitalize()}"
    val androidResourcesDir = project.layout.buildDirectory.dir("${taskIdentifier}ResourcesForAndroid")
    // FIXME: Outputting resources as a directory may result is random junk being published. Should this task do clean-up before copying?
    // This is a shim task to pass to srcDir
    val copyResourcesTask = project.tasks.register("${taskIdentifier}ShimCopyAndroidResources") {
        it.outputs.dir(androidResourcesDir)
    }

    compilation.registerCopyComposeResourcesTasks(
        "${taskIdentifier}ResourcesForAndroid",
        target.composeResourceDirectories,
        androidResourcesDir
    ).forEach { copyTask ->
        copyResourcesTask.dependsOn(copyTask)
    }

    project.androidExtension.sourceSets.getByName("main").resources.srcDir(copyResourcesTask)
}