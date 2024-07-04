/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import javax.inject.Inject

@DisableCachingByDefault
abstract class AggregateResourcesTask : DefaultTask() {

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val resourcesFromDependenciesDirectory: DirectoryProperty

    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val resourcesFromSelfDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyResources() {
        outputDirectory.get().asFile.deleteDirectoryContents()
        fileSystem.copy { copy ->
            resourcesFromDependenciesDirectory.orNull?.let { copy.from(it) }
            resourcesFromSelfDirectory.orNull?.let { copy.from(it) }
            copy.into(outputDirectory)
            copy.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    }

}