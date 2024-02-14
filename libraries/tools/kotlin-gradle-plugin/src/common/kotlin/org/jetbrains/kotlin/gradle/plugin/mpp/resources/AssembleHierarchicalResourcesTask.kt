/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class AssembleHierarchicalResourcesTask : DefaultTask() {

    internal data class Resource(
        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:InputFiles
        val absolutePath: Provider<File>,
        @get:Input
        val includes: List<String>,
        @get:Input
        val excludes: List<String>,
    )

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Inject
    abstract val projectLayout: ProjectLayout

    @get:Nested
    abstract val resourceDirectoriesByLevel: ListProperty<List<Resource>>

    @get:Input
    abstract val relativeResourcePlacement: Property<File>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    private data class DirectoryToCopy(
        val path: File,
        val includes: List<String>,
        val excludes: List<String>,
    )

    @TaskAction
    fun action() {
        val directoriesToCopy = SourceSetWalk<Resource, DirectoryToCopy>(
            object : FileSystem {},
            { it.absolutePath.get() },
            { DirectoryToCopy(it.absolutePath.get(), it.includes, it.excludes) }
        ).directoriesToCopy(
            resourceDirectoriesByLevel.get()
        ).unwrapOrThrow()

        val outputDirectoryFile = outputDirectory.get().asFile
        outputDirectoryFile.deleteDirectoryContents()

        fileSystem.copy { copy ->
            directoriesToCopy.forEach { dir ->
                copy.from(dir.path) { innerCopy ->
                    dir.includes.forEach {
                        innerCopy.include(it)
                    }
                    dir.excludes.forEach {
                        innerCopy.exclude(it)
                    }
                }
            }
            copy.into(outputDirectoryFile.resolve(relativeResourcePlacement.get()))
            copy.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        if (outputDirectoryFile.listFiles()?.isEmpty() != false) {
            // Output an empty directory for the zip task
            outputDirectoryFile.resolve(relativeResourcePlacement.get()).mkdirs()
        }
    }

    internal interface FileSystem {
        fun <T> walk(root: File, mapChildPath: (File) -> (T)): Sequence<T> = root.walk().map(mapChildPath)
        fun exists(file: File): Boolean = file.exists()
        fun isDirectory(file: File): Boolean = file.isDirectory
    }

    // FIXME: Is this replaceable with a CopySpec?
    internal class SourceSetWalk<Resource, DirectoryToCopy>(
        val fileSystem: FileSystem,
        val pathFromResource: (Resource) -> (File),
        val directoryToCopyFromResource: (Resource) -> (DirectoryToCopy),
    ) {
        sealed class Result<T> {
            data class ToCopy<T>(val value: T) : Result<T>()
            data class Collision<T>(val left: File, val right: File) : Result<T>()
            data class NotAbsolute<T>(val path: File) : Result<T>()
            data class NotDirectory<T>(val path: File) : Result<T>()

            fun unwrapOrThrow(): T {
                when (this) {
                    is Collision -> error("There is a duplicate resource in a source set level:\n${left.canonicalPath}\n${right.canonicalPath}")
                    is NotAbsolute -> error("Path $path is not absolute")
                    is NotDirectory -> error("Path $path is not a directory")
                    is ToCopy -> return value
                }
            }
        }

        fun directoriesToCopy(
            leveledResources: List<List<Resource>>
        ): Result<List<DirectoryToCopy>> {
            // 1. Is case of a collision between levels, files seen at next levels overwrite files from previous levels. E.g. iosMain/res/foo
            // resources overwrites commonMain/res/foo
            val directoriesToCopy = mutableListOf<DirectoryToCopy>()
            leveledResources.onEach { level ->
                val relativePathsSeenAtThisLevel: MutableMap<String, File> = mutableMapOf()
                level.onEach { resource ->
                    when (
                        val result = discoverAndAppendResourceDirectory(
                            resource = resource,
                            relativePathsSeenAtThisLevel = relativePathsSeenAtThisLevel,
                        )
                    ) {
                        is Result.Collision -> return Result.Collision(result.left, result.right)
                        is Result.NotAbsolute -> return Result.NotAbsolute(result.path)
                        is Result.NotDirectory -> return Result.NotDirectory(result.path)
                        is Result.ToCopy -> directoriesToCopy.add(result.value)
                    }
                }
            }

            return Result.ToCopy(directoriesToCopy)
        }

        private fun discoverAndAppendResourceDirectory(
            resource: Resource,
            relativePathsSeenAtThisLevel: MutableMap<String, File>,
        ): Result<DirectoryToCopy>? {
            val resourcesDirectory = pathFromResource(resource)
            if (!fileSystem.exists(resourcesDirectory)) return null
            if (!resourcesDirectory.isAbsolute) return Result.NotAbsolute(resourcesDirectory)
            if (!fileSystem.isDirectory(resourcesDirectory)) return Result.NotDirectory(resourcesDirectory)

            val collisions: List<Result.Collision<DirectoryToCopy>> =
                fileSystem.walk<Result.Collision<DirectoryToCopy>?>(resourcesDirectory) mapChildren@{ child ->
                    if (fileSystem.isDirectory(child)) {
                        return@mapChildren null
                    }
                    val relativePath = child.toRelativeString(resourcesDirectory)
                    relativePathsSeenAtThisLevel[relativePath]?.let { alreadySeenFile ->
                        return@mapChildren Result.Collision(
                            child,
                            alreadySeenFile,
                        )
                    }
                    relativePathsSeenAtThisLevel[relativePath] = child
                    return@mapChildren null
                }.mapNotNull { it }.toList()

            if (collisions.isNotEmpty()) {
                return collisions.first()
            }

            return Result.ToCopy(directoryToCopyFromResource(resource))
        }

    }

}