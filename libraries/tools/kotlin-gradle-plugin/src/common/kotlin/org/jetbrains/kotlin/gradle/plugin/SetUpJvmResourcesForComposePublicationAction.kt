/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.dependsOn

// FIXME: See Android Publication FIXMEs. Could the logic between android and jvm be unified?
internal val SetUpJvmResourcesForComposePublicationAction = KotlinProjectSetupAction {
    project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
        project.multiplatformExtension.targets
            .filterIsInstance<KotlinJvmTarget>()
            .forEach { target ->
                setUpPublication(target)
            }
    }
}

private fun setUpPublication(target: KotlinJvmTarget) {
    val project = target.project
    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    val taskIdentifier = target.disambiguationClassifier ?: target.name
    val resourcesBaseCopyDirectory = project.layout.buildDirectory.dir("${taskIdentifier}ResourcesForJvm")
    val copyResourcesTask = project.tasks.register("${taskIdentifier}ShimCopyJvmResources") {
        it.outputs.dir(resourcesBaseCopyDirectory)
    }

    mainCompilation.registerCopyComposeResourcesTasks(
        "${taskIdentifier}ResourcesForJvmCompilation",
        target.composeResourceDirectories,
        resourcesBaseCopyDirectory
    ).forEach { copyTask ->
        copyResourcesTask.dependsOn(copyTask)
    }

    mainCompilation.defaultSourceSet.resources.srcDir(copyResourcesTask)
}