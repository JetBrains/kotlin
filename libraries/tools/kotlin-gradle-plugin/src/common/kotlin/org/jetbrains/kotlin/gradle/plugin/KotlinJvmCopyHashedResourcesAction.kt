/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.dependsOn

internal val KotlinJvmCopyHashedResourcesAction = KotlinProjectSetupAction {
    project.launch {
        project.kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations)
        project.multiplatformExtension.targets
            .filterIsInstance<KotlinJvmTarget>()
            .forEach { target ->
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                val taskIdentifier = target.name.capitalize()
                val resourcesCopyOutput = project.layout.buildDirectory.dir("resourcesForJvm${taskIdentifier}")

                val copyResourcesTask = mainCompilation.registerCopyHashedResourcesTask<Copy>(
                    "copyResourcesForJvmCompilation${taskIdentifier}",
                    resourcesCopyOutput.map { it.dir("multiplatform-resources") }
                )
                project.tasks.named(mainCompilation.processResourcesTaskName).dependsOn(copyResourcesTask)

                val sourceSet = mainCompilation.defaultSourceSet
                sourceSet.resources.srcDir(resourcesCopyOutput)
            }
    }
}