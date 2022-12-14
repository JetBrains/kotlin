/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.plugin.ide.KlibExtra
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.commonizerTarget
import org.jetbrains.kotlin.library.resolveSingleFileKlib

fun resolveCinteropDependencies(project: Project, cinteropFiles: FileCollection): Set<IdeaKotlinDependency> {
    return cinteropFiles.files
        .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
        .mapNotNullTo(mutableSetOf()) { libraryFile ->
            createCinteropLibraryDependency(project, libraryFile)
        }
}

private fun createCinteropLibraryDependency(project: Project, libraryFile: java.io.File): IdeaKotlinResolvedBinaryDependency? {
    val library = try {
        resolveSingleFileKlib(
            libraryFile = File(libraryFile.absolutePath),
            strategy = ToolingSingleFileKlibResolveStrategy
        )
    } catch (error: Throwable) {
        project.logger.error("Failed to resolve library ${libraryFile.path}", error)
        return null
    }

    return IdeaKotlinResolvedBinaryDependency(
        binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
        binaryFile = libraryFile,
        coordinates = IdeaKotlinBinaryCoordinates(
            group = project.group.toString(),
            module = library.moduleDisplayName,
            version = null,
            sourceSetName = library.commonizerTarget
        ),
    ).apply {
        klibExtra = KlibExtra(library)
    }
}
