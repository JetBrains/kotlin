/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import java.io.File
import javax.inject.Inject

internal abstract class AssembleHierarchicalResourcesTask : DefaultTask() {

    internal data class Resource(
        @get:Internal
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

    // Track these directories as inputs
    @get:InputFiles
    val resourceDirectories: FileCollection get() = projectLayout.files(
        resourceDirectoriesByLevel.map {
            it.flatten().map { it.absolutePath }
        }
    )

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
        // 1. Is case of a collision between levels, files seen at next levels overwrite files from previous levels. E.g. iosMain/res/foo resources overwrites commonMain/res/foo
        val relativePathsSeenAtPreviousLevels: MutableSet<String> = mutableSetOf()
        val directoriesToCopy = mutableListOf<DirectoryToCopy>()
        resourceDirectoriesByLevel.get().forEach { level ->
            val relativePathsSeenAtThisLevel: MutableMap<String, File> = mutableMapOf()
            level.forEach { resourcesDirectoryDesc ->
                discoverAndAppendResourceDirectory(
                    resource = resourcesDirectoryDesc,
                    relativePathsSeenAtPreviousLevels = relativePathsSeenAtPreviousLevels,
                    relativePathsSeenAtThisLevel = relativePathsSeenAtThisLevel,
                    directoriesToCopy = directoriesToCopy,
                )
            }
            relativePathsSeenAtPreviousLevels.addAll(relativePathsSeenAtThisLevel.keys)
        }

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

        // FIXME: ???
        if (outputDirectoryFile.listFiles()?.isEmpty() != false) {
            // Output an empty directory for the zip task because otherwise Component publication breaks
            // FIXME: zipTree doesn't get copied if the zip is empty
            outputDirectoryFile.resolve(relativeResourcePlacement.get()).mkdirs()
        }
    }

    // FIXME: Обсудить коллизии с неявными зависимостями
//    jvmMain
//        -> bar/res/x *
//        -> foo/
//            -> commonMain/res/x

    // FIXME: Aggregate all errors and then throw
    private fun discoverAndAppendResourceDirectory(
        resource: Resource,
        // FIXME: Should this be a Set<File>?
        relativePathsSeenAtPreviousLevels: Set<String>,
        relativePathsSeenAtThisLevel: MutableMap<String, File>,
        directoriesToCopy: MutableList<DirectoryToCopy>,
    ) {
        val resourcesDirectory = resource.absolutePath.get()
        if (!resourcesDirectory.exists()) return
        if (!resourcesDirectory.isAbsolute) error("Path ${resourcesDirectory} is not absolute")
        if (!resourcesDirectory.isDirectory) error("Path ${resourcesDirectory} is not a directory")

        // FIXME: Validate this actually works
        // FIXME: Is resourcesDirectory.walk() deterministic?
        resourcesDirectory.walk().forEach walkChildren@{ child ->
            val relativePath = child.toRelativeString(resourcesDirectory)
            if (relativePath in relativePathsSeenAtPreviousLevels) {
                return@walkChildren
            }
            if (relativePath in relativePathsSeenAtThisLevel.keys) {
                error("There is a duplicate resource in a source set level:\n${child.canonicalPath}\n${relativePathsSeenAtThisLevel[relativePath]!!.canonicalPath}")
            }
            relativePathsSeenAtThisLevel[relativePath] = resourcesDirectory
        }
        directoriesToCopy.add(
            DirectoryToCopy(
                resourcesDirectory,
                resource.includes,
                resource.excludes,
            )
        )
    }

}