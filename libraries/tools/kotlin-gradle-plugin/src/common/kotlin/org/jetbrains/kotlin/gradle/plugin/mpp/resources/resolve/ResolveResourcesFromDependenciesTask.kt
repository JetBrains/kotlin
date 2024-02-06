/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import javax.inject.Inject

internal abstract class ResolveResourcesFromDependenciesTask : DefaultTask() {

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:InputFiles
    abstract val archivesFromDependencies: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyResources() {
        outputDirectory.get().asFile.deleteDirectoryContents()
        fileSystem.copy { copy ->
            // FIXME: Check resource is a kotlin_resources.zip file?
            archivesFromDependencies.filter { it.exists() }.forEach {
                copy.from(archiveOperations.zipTree(it))
            }
            copy.into(outputDirectory)
            // Copy empty directories for a target without resources, but with resources publication
            // FIXME: This doesn't work
            // copy.includeEmptyDirs = true
            copy.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    }

}