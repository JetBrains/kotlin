/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import java.io.File
import java.nio.file.Paths
import java.util.*

internal fun File.isJavaFile() =
    extension.equals("java", ignoreCase = true)

internal fun File.isKotlinFile(sourceFilesExtensions: List<String>): Boolean =
    !isJavaFile() && sourceFilesExtensions.any { it.equals(extension, ignoreCase = true) }

internal fun File.isClassFile(): Boolean =
    extension.equals("class", ignoreCase = true)

internal fun File.relativeOrCanonical(base: File): String =
    relativeToOrNull(base)?.path ?: canonicalPath

internal fun Iterable<File>.pathsAsStringRelativeTo(base: File): String =
    map { it.relativeOrCanonical(base) }.sorted().joinToString()

internal fun File.relativeToRoot(project: Project): String =
    relativeOrCanonical(project.rootProject.rootDir)

internal fun Iterable<File>.toSortedPathsArray(): Array<String> =
    map { it.canonicalPath }.toTypedArray().also { Arrays.sort(it) }

internal fun newTmpFile(prefix: String, suffix: String? = null, directory: File? = null, deleteOnExit: Boolean = true) =
    File.createTempFile(prefix, suffix, directory).apply {
        if (deleteOnExit) deleteOnExit()
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