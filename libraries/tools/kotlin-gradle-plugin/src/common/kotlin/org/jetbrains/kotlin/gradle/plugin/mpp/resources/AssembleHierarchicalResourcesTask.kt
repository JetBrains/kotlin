/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
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

    internal class Resource(
        @get:Internal
        val resourcesBaseDirectory: Provider<File>,

        includes: List<String>,
        excludes: List<String>,
        layout: ProjectLayout,
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val resourcesFileTree: Provider<FileTree> = layout.dir(resourcesBaseDirectory).map { baseDirectory ->
            baseDirectory.asFileTree.matching { patternFilter ->
                includes.forEach { patternFilter.include(it) }
                excludes.forEach { patternFilter.exclude(it) }
            }
        }
    )

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Inject
    abstract val projectLayout: ProjectLayout

    @get:Nested
    abstract val resourceDirectoriesByLevel: ListProperty<List<Resource>>

    /**
     * Relative resource placement is part of this task to prepare the output directory as is for consumption by the .zip publication or the
     * self-resources resolution tasks
     */
    @get:Input
    abstract val relativeResourcePlacement: Property<File>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun action() {
        val directoriesToCopy = resourcesSourceSetWalk.directoriesToCopy(
            resourceDirectoriesByLevel.get()
        ).unwrapOrThrow()

        val outputDirectoryFile = outputDirectory.get().asFile
        outputDirectoryFile.deleteDirectoryContents()

        fileSystem.copy { copy ->
            directoriesToCopy.forEach { dir ->
                copy.from(dir)
            }
            copy.into(outputDirectoryFile.resolve(relativeResourcePlacement.get()))
            copy.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        if (outputDirectoryFile.listFiles()?.isEmpty() != false) {
            // Output an empty directory for the zip task
            outputDirectoryFile.resolve(relativeResourcePlacement.get()).mkdirs()
        }
    }

    private val resourcesSourceSetWalk = SourceSetWalk(
        fileSystem = object : FileSystem<Resource> {
            override fun walk(root: Resource): Sequence<File> = root.resourcesFileTree.get().asSequence()
        },
        basePathFromResource = { it.resourcesBaseDirectory.get() },
        fileTreeToCopyFromResource = { it.resourcesFileTree.get() },
    )

    internal interface FileSystem<RootEntity> {
        fun walk(root: RootEntity): Sequence<File>
        fun exists(file: File): Boolean = file.exists()
        fun isDirectory(file: File): Boolean = file.isDirectory
    }

    internal class SourceSetWalk<Resource, FileTreeToCopy>(
        val fileSystem: FileSystem<Resource>,
        val basePathFromResource: (Resource) -> (File),
        val fileTreeToCopyFromResource: (Resource) -> (FileTreeToCopy),
    ) {
        sealed class Result<T> {
            data class ToCopy<T>(val value: T) : Result<T>()
            data class Collision<T>(val left: File, val right: File) : Result<T>()
            data class NotDirectory<T>(val path: File) : Result<T>()

            fun unwrapOrThrow(): T {
                when (this) {
                    is Collision -> error("There is a duplicate resource in a source set level:\n${left.canonicalPath}\n${right.canonicalPath}")
                    is NotDirectory -> error("Path $path is not a directory")
                    is ToCopy -> return value
                }
            }
        }

        fun directoriesToCopy(
            leveledResources: List<List<Resource>>
        ): Result<List<FileTreeToCopy>> {
            // 1. Is case of a collision between levels, files seen at next levels overwrite files from previous levels. E.g. iosMain/res/foo
            // resources overwrites commonMain/res/foo
            val directoriesToCopy = mutableListOf<FileTreeToCopy>()
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
        ): Result<FileTreeToCopy>? {
            val resourcesDirectory = basePathFromResource(resource)
            if (!fileSystem.exists(resourcesDirectory)) return null
            if (!fileSystem.isDirectory(resourcesDirectory)) return Result.NotDirectory(resourcesDirectory)

            val collisions: List<Result.Collision<FileTreeToCopy>> =
                fileSystem.walk(resource).map<File, Result.Collision<FileTreeToCopy>?>{ child ->
                    if (fileSystem.isDirectory(child)) {
                        return@map null
                    }
                    val relativePath = child.toRelativeString(resourcesDirectory)
                    relativePathsSeenAtThisLevel[relativePath]?.let { alreadySeenFile ->
                        return@map Result.Collision(
                            child,
                            alreadySeenFile,
                        )
                    }
                    relativePathsSeenAtThisLevel[relativePath] = child
                    return@map null
                }.mapNotNull { it }.toList()

            if (collisions.isNotEmpty()) {
                return collisions.first()
            }

            return Result.ToCopy(fileTreeToCopyFromResource(resource))
        }

    }

}