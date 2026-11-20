/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.file.RelativePath
import java.io.File

/**
 * Copies [subdirectory] of [files] into [destination].
 *
 * E.g. fileTree contains following
 *  foo/bar/x.txt
 *  foo/bar/y.txt
 *  foo/baz.txt
 *
 * Calling function
 *  `copySubDirectoryContent(fileTree, "foo", file("/tmp/xxx"))`
 *  will copy everything from foo into tmp/xxx
 *  i.e.
 *  /tmp/xxx/bar/x.txt
 *  /tmp/xxx/bar/y.txt
 *  /tmp/xxx/baz.txt
 */
internal fun FileSystemOperations.copySubDirectoryContent(files: FileTree, subdirectory: String, destination: File) {
    val subdirectory = if (!subdirectory.endsWith("/")) "$subdirectory/" else subdirectory
    copy { copy ->
        val subdirectoryFiles = files.matching { it.include("$subdirectory**") }
        copy.from(subdirectoryFiles) { wit ->
            wit.includeEmptyDirs = false
            wit.eachFile { file ->
                file.relativePath = RelativePath.parse(
                    !file.isDirectory,
                    // removePrefix on relativePath throws StackOverflowError
                    // because during stringification RelativePath calls StringCharBuffer, which calls subSequence
                    // so first necessary to stringify it
                    file.relativePath.toString().removePrefix(subdirectory)
                )
            }
        }
        copy.into(destination)
    }
}
