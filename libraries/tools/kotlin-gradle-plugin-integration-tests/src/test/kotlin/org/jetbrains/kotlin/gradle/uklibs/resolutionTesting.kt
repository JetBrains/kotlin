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
import java.io.File
import kotlin.io.path.pathString

fun <T> Project.ignoreAccessViolations(code: () -> (T)) = (project.gradle as GradleInternal).services.get(
    ProjectStateRegistry::class.java).allowUncontrolledAccessToAnyProject { code() }

fun TestProject.metadataTransformationOutputClasspath(
    sourceSetName: String,
): List<File> {
    val iosMainTransformationTask = buildScriptReturn {
        project.locateOrRegisterMetadataDependencyTransformationTask(
            kotlinMultiplatform.sourceSets.getByName(sourceSetName)
        ).name
    }.buildAndReturn()
    val outputClasspath = buildScriptReturn {
        val transformationTask = project.locateOrRegisterMetadataDependencyTransformationTask(
            kotlinMultiplatform.sourceSets.getByName(sourceSetName)
        ).get()
        transformationTask.allTransformedLibraries().get()
    }.buildAndReturn(iosMainTransformationTask)
    return outputClasspath
}

fun List<File>.relativeTransformationPathComponents(): List<List<String>> = map { it.lastPathComponents(2) }
private fun File.lastPathComponents(number: Int): List<String> = toPath().toList().takeLast(number).map { it.pathString }