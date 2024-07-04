/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import javax.inject.Inject

@DisableCachingByDefault
abstract class ResolveResourcesFromDependenciesTask : DefaultTask() {

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Input
    abstract val filterResourcesByExtension: Property<Boolean>

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
                .filter { it.isFile }
                .filter { if (filterResourcesByExtension.get()) it.name.endsWith(KotlinTargetResourcesPublicationImpl.RESOURCES_ZIP_EXTENSION) else true }
                .forEach {
                    copy.from(archiveOperations.zipTree(it))
                }
            copy.into(outputDirectory)
            copy.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    }

}