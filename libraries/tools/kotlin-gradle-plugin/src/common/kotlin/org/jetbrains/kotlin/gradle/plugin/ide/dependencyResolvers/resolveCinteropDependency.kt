/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.plugin.ide.KlibExtra
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.*

internal fun Project.resolveCInteropDependencies(cinteropFiles: Iterable<java.io.File>): Set<IdeaKotlinDependency> {
    return cinteropFiles
        .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
        .mapNotNull { libraryFile -> this.createCinteropLibraryDependency(libraryFile) }
        .toSet()
}

private fun Project.createCinteropLibraryDependency(libraryFile: java.io.File): IdeaKotlinBinaryDependency? {
    if (!libraryFile.exists()) {
        return IdeaKotlinUnresolvedBinaryDependency(
            cause = "cinterop file: ${libraryFile.path} does not exist",
            coordinates = null
        )
    }

    val library = try {
        resolveSingleFileKlib(
            libraryFile = File(libraryFile.absolutePath),
            strategy = ToolingSingleFileKlibResolveStrategy
        )
    } catch (error: Throwable) {
        logger.error("Failed to resolve library ${libraryFile.path}", error)
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
