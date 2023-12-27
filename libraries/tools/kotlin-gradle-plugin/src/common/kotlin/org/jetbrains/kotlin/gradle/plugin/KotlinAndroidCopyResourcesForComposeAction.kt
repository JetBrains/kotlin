/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.androidExtension


internal val KotlinAndroidCopyResourcesForComposeAction = KotlinProjectSetupAction {
    val project = this
    launch {
        project.kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations)
        project.multiplatformExtension.targets.filterIsInstance<KotlinAndroidTarget>().forEach { target ->
            target.publishLibraryVariants.orEmpty().forEach { variant ->
                val compilation = target.compilations.getByName(variant)
                val taskIdentifier = "${target.name.capitalize()}${variant.capitalize()}"
                val androidResourcesDir = project.layout.buildDirectory.dir("resourcesForAndroid${taskIdentifier}")
                // shim task for srcDir
                val copyResourcesTask = project.tasks.register("shimCopyAndroidResources${taskIdentifier}") {
                    it.outputs.dir(androidResourcesDir)
                }

                target.composeResourceDirectories.forEach { resources ->
                    val copyTask = compilation.registerCopyResourcesTask<Copy>(
                        "ResourcesForAndroid${taskIdentifier}",
                        resources,
                        androidResourcesDir,
                    )
                    copyResourcesTask.configure {
                        it.dependsOn(copyTask)
                    }
                }

                project.androidExtension.sourceSets.getByName("main").resources.srcDir(copyResourcesTask)
            }
        }
    }
}