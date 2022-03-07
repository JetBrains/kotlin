/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

internal fun File.isJavaFile() =
    extension.equals("java", ignoreCase = true)

internal fun File.isKotlinFile(sourceFilesExtensions: List<String>): Boolean =
    !isJavaFile() && sourceFilesExtensions.any { it.equals(extension, ignoreCase = true) }

internal fun File.relativeOrCanonical(base: File): String =
    relativeToOrNull(base)?.path ?: canonicalPath

internal fun Iterable<File>.pathsAsStringRelativeTo(base: File): String =
    map { it.relativeOrCanonical(base) }.sorted().joinToString()

internal fun File.relativeToRoot(project: Project): String =
    relativeOrCanonical(project.rootProject.rootDir)

internal fun Iterable<File>.toPathsArray(): Array<String> =
    map { it.canonicalPath }.toTypedArray()

internal fun newTmpFile(prefix: String, suffix: String? = null, directory: File? = null, deleteOnExit: Boolean = true): File {
    return try {
        (if (directory == null) Files.createTempFile(prefix, suffix) else Files.createTempFile(directory.toPath(), prefix, suffix))
    } catch (e: NoSuchFileException) {
        val parentDir = e.file.parentFile

        if (parentDir.isFile) throw IOException("Temp folder $parentDir is not a directory")
        if (!parentDir.isDirectory) {
            if (!parentDir.mkdirs()) throw IOException("Could not create temp directory $parentDir")
        }

        Files.createTempFile(parentDir.toPath(), prefix, suffix)
    }.toFile().apply { if (deleteOnExit) deleteOnExit() }
}

internal fun File.isParentOf(childCandidate: File, strict: Boolean = false): Boolean {
    val parentPath = Paths.get(this.absolutePath).normalize()
    val childCandidatePath = Paths.get(childCandidate.absolutePath).normalize()

    return if (strict) {
        childCandidatePath.startsWith(parentPath) && parentPath != childCandidate
    } else {
        childCandidatePath.startsWith(parentPath)
    }
}

internal fun File.canonicalPathWithoutExtension(): String =
    canonicalPath.substringBeforeLast(".")

internal fun File.listFilesOrEmpty() = (if (exists()) listFiles() else null).orEmpty()

internal inline fun <T> withTemporaryDirectory(prefix: String, action: (directory: File) -> T): T {
    val directory = Files.createTempDirectory(prefix).toFile()
    return try {
        action(directory)
    } finally {
        directory.deleteRecursively()
    }
}
