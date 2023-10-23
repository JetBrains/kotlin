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

    @InputFiles @PathSensitive(PathSensitivity.RELATIVE) val files = mutableSetOf<FileCollection>()

    @Input val namedKlibs = mutableSetOf<String>()

    @Internal val artifacts = mutableListOf<KonanBuildingTask>()

    val artifactFiles: List<File>
        @InputFiles @PathSensitive(PathSensitivity.RELATIVE) get() = artifacts.map { it.artifact }

    @Internal val explicitRepos = mutableSetOf<File>()

    val repos: Set<File>
        @Input get() = mutableSetOf<File>().apply {
            addAll(explicitRepos)
            add(task.destinationDir) // TODO: Check if task is a library - create a Library interface
            add(task.destinationDir) // TODO: Check if task is a library - create a Library interface
            add(task.project.konanLibsBaseDir.targetSubdir(target))
            addAll(artifacts.flatMap { it.libraries.repos })
            addAll(task.platformConfiguration.files.map { it.parentFile })
        }

    val target: KonanTarget
        @Internal get() = task.konanTarget

    private val friendsTasks = mutableSetOf<KonanBuildingTask>()

    @get:Internal // Taken into account by tasks's dependOn.
    val friends: Set<File> get() = mutableSetOf<File>().apply {
        addAll(friendsTasks.map { it.artifact })
    }

    // DSL Methods

    /** Absolute path */
    fun file(file: Any)                   = files.add(project.files(file))
    fun files(vararg files: Any)          = this.files.addAll(files.map { project.files(it) })
    fun files(collection: FileCollection) = this.files.add(collection)

    /** The compiler with search the library in repos */
    fun klib(lib: String)             = namedKlibs.add(lib)
    fun klibs(vararg libs: String)    = namedKlibs.addAll(libs)
    fun klibs(libs: Iterable<String>) = namedKlibs.addAll(libs)

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

    /** Add repo for library search */
    fun useRepo(directory: Any) = explicitRepos.add(project.file(directory))
    /** Add repos for library search */
    fun useRepos(vararg directories: Any) = directories.forEach { useRepo(it) }
    /** Add repos for library search */
    fun useRepos(directories: Iterable<Any>) = directories.forEach { useRepo(it) }

    private fun Project.evaluationDependsOn(another: Project) {
        if (this != another) { evaluationDependsOn(another.path) }
    }

    fun asFiles(): List<File> = asFiles(
        defaultResolver(
            repos.map { it.absolutePath },
            task.konanTarget,
            Distribution(project.konanHome)
        )
    )

    fun asFiles(resolver: SearchPathResolver<*>): List<File> = mutableListOf<File>().apply {
        files.flatMapTo(this) { it.files }
        addAll(artifactFiles)
        addAll(task.platformConfiguration.files)
        namedKlibs.mapTo(this) { project.file(resolver.resolve(it).libraryFile.absolutePath) }
    }
}
