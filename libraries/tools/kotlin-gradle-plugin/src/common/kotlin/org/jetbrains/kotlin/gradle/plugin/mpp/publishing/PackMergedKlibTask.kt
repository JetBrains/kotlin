/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

private const val PACK_MERGED_KLIB_TASK_NAME = "packMergedKlibTask"

internal val SetupMergedKlibTask = KotlinProjectSetupCoroutine {
    val extension = project.multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine
    val compileKlibTasks = extension.awaitTargets().associateWith { target ->
        when (target) {
            is KotlinNativeTarget -> target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).compileTaskProvider
            is KotlinJsIrTarget -> target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).compileTaskProvider
            else -> null
        }
    }

     tasks.register(PACK_MERGED_KLIB_TASK_NAME, Zip::class.java) { zip ->
        compileKlibTasks.forEach { (target, compileTaskProvider) ->
            val compileTask = compileTaskProvider?.get() ?: return@forEach
            zip.into("platform/${target.name}") { spec ->
                if (compileTask.produceUnpackagedKlib.get()) {
                    spec.from(compileTask.klibDirectory)
                } else {
                    spec.from(zipTree(compileTask.klibOutput))
                }
            }
        }

        zip.destinationDirectory.set(layout.buildDirectory.dir("merged-klib"))
        zip.archiveFileName.set("merged-klib.zip")
    }
}

internal val Project.packMergedKlibTask: TaskProvider<Task>
    get() = tasks.named(PACK_MERGED_KLIB_TASK_NAME)


