/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import java.io.File

object SourceSetMetadataStorageForIde {
    fun cleanupStaleEntries(project: Project) {
        val projectStorageDirectories = project.rootProject.allprojects.associateBy { projectStorage(it) }
        getStorageRoot(project).listFiles().orEmpty().filter { it.isDirectory }.forEach { directory ->
            // If no project corresponds to the directory, remove the directory
            if (directory !in projectStorageDirectories) {
                directory.deleteRecursively()
            } else {
                // Under the project's directory, delete subdirectories that don't correspond to any source set:
                val sourceSets = projectStorageDirectories.getValue(directory)?.project?.multiplatformExtensionOrNull?.sourceSets.orEmpty()
                val sourceSetNames = sourceSets.map { it.name }
                directory.listFiles().orEmpty().filter { it.isDirectory }.forEach { subdirectory ->
                    if (subdirectory.name !in sourceSetNames)
                        subdirectory.deleteRecursively()
                }
            }
        }
    }

    private fun getStorageRoot(project: Project): File = project.rootDir.resolve(".gradle/kotlin/sourceSetMetadata")

    private fun projectStorage(project: Project): File {
        val projectPathSegments = generateSequence(project) { it.parent }.map { it.name }
        return getStorageRoot(project).resolve(
            // Escape dots in project names to avoid ambiguous paths.
            projectPathSegments.joinToString(".") { it.replace(".", "_.") }
        )
    }

    fun sourceSetStorage(project: Project, sourceSetName: String) = projectStorage(project).resolve(sourceSetName)
}