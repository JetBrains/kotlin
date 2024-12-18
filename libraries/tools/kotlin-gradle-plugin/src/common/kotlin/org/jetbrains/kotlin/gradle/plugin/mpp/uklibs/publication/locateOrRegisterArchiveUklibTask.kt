/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.locateTask

internal suspend fun Project.locateOrRegisterArchiveUklibTask(): TaskProvider<ArchiveUklibTask> {
    val taskName = "archiveUklib"
    tasks.locateTask<ArchiveUklibTask>(taskName)?.let { return it }

    val archiveUklib = tasks.register(taskName, ArchiveUklibTask::class.java)

    val kgpFragments = multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments()

    kgpFragments.forEach { fragment ->
        archiveUklib.configure {
            // outputFile might be a directory or a file
            it.inputs.files(fragment.outputFile)
            // FIXME: some outputFiles are derived from a project.provider, use explicit task dependency as a temporary workaround
            it.dependsOn(fragment.providingTask)
        }
    }

    archiveUklib.configure {
        it.fragmentsWithTransitiveRefinees.set(
            kgpFragments.map {
                it.fragment to it.refineesTransitiveClosure
            }.toMap()
        )
    }

    return archiveUklib
}

internal suspend fun Project.locateOrStubJvmJarTask(): TaskProvider<Jar> {
    val jvmTarget = project.multiplatformExtension.awaitTargets().singleOrNull {
        it is KotlinJvmTarget
    }
    return if (jvmTarget != null) {
        @Suppress("UNCHECKED_CAST")
        project.tasks.named(jvmTarget.artifactsTaskName) as TaskProvider<Jar>
    } else {
        val stubTaskName = "stubJvmJar"
        tasks.locateTask<Jar>(stubTaskName) ?: project.tasks.register(stubTaskName, Jar::class.java)
    }
}