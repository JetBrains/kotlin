/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class ResolveResourcesFromDependenciesTask : DefaultTask() {

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val archivesFromDependencies: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyResources() {
        outputDirectory.get().asFile.deleteDirectoryContents()
        fileSystem.copy { copy ->
            archivesFromDependencies
                .filter { it.exists() }
                // FIXME: Remove the zip filtering when these two issues are resolved:
                // 1. wasm passes self-classes directory somewhere in the api configuration
                // 2. current attributes resolve to klibs
                .filter { it.extension == "zip" }
                .forEach {
                    copy.from(archiveOperations.zipTree(it))
                }
            copy.into(outputDirectory)
            copy.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    }

}