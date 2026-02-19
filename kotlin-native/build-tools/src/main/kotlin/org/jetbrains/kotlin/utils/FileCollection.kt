/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import org.gradle.api.file.FileCollection
import java.io.File


/**
 * Whenever a `FileCollection` is passed as arguments, it's order must be stably sorted for reproducibility.
 * @param reproducibilityRootsMap Map of volatile path prefixes to the replacements to be used instead.
 */
fun FileCollection.reproduciblySortedFilePaths(reproducibilityRootsMap: Map<File, String>): List<File> = files.map { file ->
    // We cannot just sort absolute paths: they may change from machine to machine.
    // So, apply `reproducibilityCompilerFlags` to the list of files manually and use the resulting paths as keys.
    reproducibilityRootsMap.firstNotNullOfOrNull { (root, name) ->
        if (file.startsWith(root)) {
            "$name${File.separator}${file.toRelativeString(root)}" to file
        } else if (file.isAbsolute) {
            file.path to file
        } else {
            null
        }
    } ?: (file.absolutePath to file) // TODO: consider erroring out instead
}.sortedBy { it.first }.map { it.second }
