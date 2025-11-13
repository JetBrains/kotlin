/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.api.Project
import org.gradle.api.internal.GradleInternal
import org.gradle.api.internal.project.ProjectStateRegistry
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import org.jetbrains.kotlin.gradle.testbase.providerBuildScriptReturn
import java.io.File
import kotlin.io.path.pathString

/**
 * buildScriptReturn injections execute in the FlowAction build finish callback. Unfortunately Gradle prohibits resolving configurations
 * there, but we can use this workaround to suppress the check
 */
fun <T : Any> Project.ignoreAccessViolations(code: () -> (T)) = (project.gradle as GradleInternal).services.get(
    ProjectStateRegistry::class.java
).allowUncontrolledAccessToAnyProject { code() }

fun TestProject.metadataTransformationOutputClasspath(
    sourceSetName: String,
): List<File> {
    val dumpTaskName = "dump_${sourceSetName}_${generateIdentifier()}"
    val outputClasspath = providerBuildScriptReturn {
        val gmtTask = project.provider {
            kotlinMultiplatform.sourceSets.getByName(sourceSetName)
        }.flatMap {
            project.locateOrRegisterMetadataDependencyTransformationTask(it)
        }
        project.tasks.register(dumpTaskName) {
            it.dependsOn(gmtTask)
        }
        gmtTask.flatMap {
            it.allTransformedLibraries()
        }
    }.buildAndReturn(dumpTaskName)
    return outputClasspath
}

fun List<File>.relativeTransformationPathComponents(componentsNumber: Int = 2): List<List<String>> = map { it.lastPathComponents(componentsNumber) }
private fun File.lastPathComponents(number: Int): List<String> = toPath().toList().takeLast(number).map { it.pathString }
