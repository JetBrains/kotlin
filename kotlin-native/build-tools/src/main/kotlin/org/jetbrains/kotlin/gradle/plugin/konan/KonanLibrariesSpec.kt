/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanArtifactWithLibrariesTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanBuildingTask
import org.jetbrains.kotlin.konan.*
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.SearchPathResolver
import java.io.File

open class KonanLibrariesSpec(
        @Internal val task: KonanArtifactWithLibrariesTask,
        @Internal val project: Project
) {

    @InputFiles @PathSensitive(PathSensitivity.RELATIVE) val klibFiles = mutableSetOf<FileCollection>()

    @Internal val artifacts = mutableListOf<KonanBuildingTask>()

    val artifactFiles: List<File>
        @InputFiles @PathSensitive(PathSensitivity.RELATIVE) get() = artifacts.map { it.artifact }

    val target: KonanTarget
        @Internal get() = task.konanTarget

    private val friendsTasks = mutableSetOf<KonanBuildingTask>()

    @get:Internal // Taken into account by tasks's dependOn.
    val friends: Set<File> get() = mutableSetOf<File>().apply {
        addAll(friendsTasks.map { it.artifact })
    }

    // DSL Methods

    /** Absolute path */
    fun klibFile(file: Any)                   { klibFiles.add(project.files(file)) }
    fun klibFiles(vararg files: Any)          { klibFiles.addAll(files.map { project.files(it) }) }
    fun klibFiles(collection: FileCollection) { klibFiles.add(collection) }

    private fun klibInternal(lib: KonanBuildingConfig<*>, friend: Boolean) {
        if (!(lib is KonanLibrary || lib is KonanInteropLibrary)) {
            throw InvalidUserDataException("Config ${lib.name} is not a library")
        }

        val libraryTask = lib[target]?.get() ?:
            throw InvalidUserDataException("Library ${lib.name} has no target ${target.visibleName}")

        if (libraryTask == task) {
            throw InvalidUserDataException("Attempt to use a library as its own dependency: " +
                    "${task.name} (in project: ${project.path})")
        }
        artifacts.add(libraryTask)
        task.dependsOn(libraryTask)
        if (friend) friendsTasks.add(libraryTask)
    }

    /** Direct link to a config */
    fun klib(lib: KonanLibrary) = klibInternal(lib, false)
    /** Direct link to a config */
    fun klib(lib: KonanInteropLibrary) = klibInternal(lib, false)

    /** Artifact in the specified project by name */
    fun artifact(libraryProject: Project, name: String, friend: Boolean) {
        project.evaluationDependsOn(libraryProject)
        klibInternal(libraryProject.konanArtifactsContainer.getByName(name), friend)
    }

    fun artifact(libraryProject: Project, name: String) = artifact(libraryProject, name, false)
    /** Artifact in the current project by name */
    fun artifact(name: String, friend: Boolean) = artifact(project, name, friend)

    fun artifact(name: String) = artifact(project, name, false)

    /** Artifact by direct link */
    fun artifact(artifact: KonanLibrary) = klib(artifact)
    /** Direct link to a config */
    fun artifact(artifact: KonanInteropLibrary) = klib(artifact)

    private fun allArtifactsFromInternal(libraryProjects: Array<out Project>,
                                         filter: (KonanBuildingConfig<*>) -> Boolean) {
        libraryProjects.forEach { prj ->
            project.evaluationDependsOn(prj)
            prj.konanArtifactsContainer.filter(filter).forEach {
                klibInternal(it, false)
            }
        }
    }

    /** All libraries (both interop and non-interop ones) from the projects by direct references  */
    fun allLibrariesFrom(vararg libraryProjects: Project) = allArtifactsFromInternal(libraryProjects) {
        it is KonanLibrary || it is KonanInteropLibrary
    }

    /** All interop libraries from the projects by direct references */
    fun allInteropLibrariesFrom(vararg libraryProjects: Project) = allArtifactsFromInternal(libraryProjects) {
        it is KonanInteropLibrary
    }

    private fun Project.evaluationDependsOn(another: Project) {
        if (this != another) { evaluationDependsOn(another.path) }
    }
}
