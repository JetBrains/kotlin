/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.nio.file.FileSystemLoopException
import java.nio.file.Path

/**
 * An enumeration to provide walk options for the [Path.walk] function.
 * The options can be combined to form the desired walk order and behavior.
 *
 * Example:
 * ```kotlin
 * val startDirectory = createTempDirectory()
 * run {
 *     (startDirectory / "1" / "2" / "3" / "4").createDirectories()
 *     (startDirectory / "1" / "2" / "3" / "a.txt").createFile()
 *     (startDirectory / "1" / "2" / "b.txt").createFile()
 *     (startDirectory / "c.txt").createFile()
 * }
 *
 * // Default walk options. Prints:
 * //    1/2/b.txt
 * //    1/2/3/a.txt
 * //    c.txt
 * startDirectory.walk().forEach { path ->
 *     println(path.relativeTo(startDirectory))
 * }
 *
 * // Custom walk options. Prints:
 * //    1
 * //    c.txt
 * //    1/2
 * //    1/2/b.txt
 * //    1/2/3
 * //    1/2/3/a.txt
 * //    1/2/3/4
 * startDirectory.walk(PathWalkOption.INCLUDE_DIRECTORIES, PathWalkOption.BREADTH_FIRST).forEach { path ->
 *     println(path.relativeTo(startDirectory))
 * }
 * ```
 *
 * Note that this enumeration is not exhaustive, and new cases might be added in the future.
 */
@WasExperimental(ExperimentalPathApi::class)
@SinceKotlin("2.1")
public enum class PathWalkOption {
    /**
     * Includes directory paths in the resulting sequence of the walk.
     *
     * By default, the [Path.walk] sequence does not include directory paths.
     * Passing this option to the function includes directory paths in the resulting sequence.
     */
    INCLUDE_DIRECTORIES,

    /**
     * Walks in breadth-first order.
     *
     * By default, the [Path.walk] sequence traverses the starting directory and all its contents in depth-first order.
     * Passing this option to the function changes the traversal to breadth-first order.
     */
    BREADTH_FIRST,

    /**
     * Follows symbolic links to the directories they point to.
     *
     * By default, the [Path.walk] sequence includes symbolic links as-is
     * and does not visit the contents of the directories they point to.
     * Passing this option to the function causes the traversal to follow symbolic links
     * and include the contents of the directories they point to in the resulting sequence.
     * Note that following symbolic links may result in cycles during traversal,
     * in which case a [FileSystemLoopException] is thrown.
     */
    FOLLOW_LINKS
}