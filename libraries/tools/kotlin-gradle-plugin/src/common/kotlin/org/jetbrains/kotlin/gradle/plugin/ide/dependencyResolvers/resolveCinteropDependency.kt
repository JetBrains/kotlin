/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.plugin.ide.KlibExtra
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.*

internal fun Project.resolveCinteropDependencies(cinteropFiles: FileCollection): Set<IdeaKotlinDependency> {
    return cinteropFiles.files
        .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
        .mapNotNullTo(mutableSetOf()) { libraryFile ->
            createCinteropLibraryDependency(this, libraryFile)
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

    val (group, module) = cinteropGroupAndModule(library)

    return IdeaKotlinResolvedBinaryDependency(
        binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
        classpath = IdeaKotlinClasspath(libraryFile),
        coordinates = IdeaKotlinBinaryCoordinates(
            group = group,
            module = module,
            version = null, // TODO (kirpichenkov): if/when used for published cinterops, should be set up correctly
            sourceSetName = library.nativeTargets.singleOrNull() ?: library.nativeTargets.joinToString(prefix = "(", postfix = ")")
        ),
    ).apply {
        klibExtra = KlibExtra(library)
    }
}

private fun cinteropGroupAndModule(library: KotlinLibrary): Pair<String, String> {
    val nameParts = library.uniqueName.split(":")

    return when (nameParts.size) {
        0 -> "<unknown>" to "<unknown>"
        1 -> "<unknown>" to nameParts.single()
        else -> nameParts[0] to nameParts[1]
    }
}
