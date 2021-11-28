/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import java.io.File

object SourceSetMetadataStorageForIde {
    fun cleanupStaleEntries(project: Project) {
        val projectStorageDirectories = project.rootProject.allprojects
            .associate { projectStorage(it) to it.multiplatformExtensionOrNull?.sourceSets.orEmpty().map { it.name } }
        cleanupStaleEntries(getStorageRoot(project), projectStorageDirectories)
    }

    fun cleanupStaleEntries(projectStorageRoot: File, projectStorageDirectories: Map<File, List<String>>) {
        projectStorageRoot.listFiles().orEmpty().filter { it.isDirectory }.forEach { directory ->
            // If no project corresponds to the directory, remove the directory
            if (directory !in projectStorageDirectories) {
                directory.deleteRecursively()
            } else {
                // Under the project's directory, delete subdirectories that don't correspond to any source set:
                val sourceSetNames = projectStorageDirectories.getValue(directory)
                directory.listFiles().orEmpty().filter { it.isDirectory }.forEach { subdirectory ->
                    if (subdirectory.name !in sourceSetNames)
                        subdirectory.deleteRecursively()
                }
            }
        }
    }

    internal fun getStorageRoot(project: Project): File = project.rootDir.resolve(".gradle/kotlin/sourceSetMetadata")

    internal fun projectStorage(project: Project): File {
        val projectPathSegments = generateSequence(project) { it.parent }.map { it.name }
        return getStorageRoot(project).resolve(
            // Escape dots in project names to avoid ambiguous paths.
            projectPathSegments.joinToString(".") { it.replace(".", "_.") }
        )
    }

    fun sourceSetStorage(project: Project, sourceSetName: String) = projectStorage(project).resolve(sourceSetName)

    internal fun sourceSetStorageWithScope(project: Project, sourceSetName: String, scope: KotlinDependencyScope) =
        sourceSetStorage(project, sourceSetName).resolve(scope.scopeName)
}

abstract class CleanupStaleSourceSetMetadataEntriesService : BuildService<CleanupStaleSourceSetMetadataEntriesService.Parameters>, AutoCloseable, OperationCompletionListener {
    interface Parameters : BuildServiceParameters {
        val projectStorageRoot: Property<File>
        val projectStorageDirectories: MapProperty<File, List<String>>
    }

    override fun onFinish(event: FinishEvent?) {
        // noop
    }

    override fun close() {
        SourceSetMetadataStorageForIde.cleanupStaleEntries(parameters.projectStorageRoot.get(), parameters.projectStorageDirectories.get())
    }

    companion object {
        fun configure(spec: BuildServiceSpec<Parameters>, project: Project) {
            spec.parameters.projectStorageRoot.set(SourceSetMetadataStorageForIde.getStorageRoot(project))
            spec.parameters.projectStorageDirectories.set(project.rootProject.allprojects.associate {
                SourceSetMetadataStorageForIde.projectStorage(it) to it.multiplatformExtensionOrNull?.sourceSets.orEmpty().map { it.name }
            })
        }
    }
}