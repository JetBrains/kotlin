/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * The builder to provide implementation of the [FileVisitor] that [fileVisitor] function builds.
 *
 * Example:
 *
 * ```kotlin
 * val cleanVisitor = fileVisitor {
 *     onPreVisitDirectory { directory, _ ->
 *         if (directory.name == "build") {
 *             directory.deleteRecursively()
 *             FileVisitResult.SKIP_SUBTREE
 *         } else {
 *             FileVisitResult.CONTINUE
 *         }
 *     }
 *
 *     onVisitFile { file, _ ->
 *         if (file.extension == "class") {
 *             file.deleteExisting()
 *         }
 *         FileVisitResult.CONTINUE
 *     }
 *
 *     onVisitFileFailed { entry, _ ->
 *         Logger.warn("Could not traverse and clean $entry")
 *         FileVisitResult.CONTINUE
 *     }
 * }
 * ```
 */
@WasExperimental(ExperimentalPathApi::class)
@SinceKotlin("2.1")
public sealed interface FileVisitorBuilder {
    /**
     * Overrides the corresponding function of the built [FileVisitor] with the provided [function].
     *
     * The provided callback is invoked for a directory before its contents are visited.
     * Depending on the return value of the function:
     *   * [FileVisitResult.CONTINUE] - The contents of the directory are visited.
     *   * [FileVisitResult.SKIP_SUBTREE] - The contents of the directory are not visited,
     *     and the traversal continues to a sibling entry.
     *   * [FileVisitResult.SKIP_SIBLINGS] - The contents of the directory are not visited,
     *     and sibling entries are also not visited.
     *   * [FileVisitResult.TERMINATE] - The traversal terminates immediately, and no further entries are visited.
     *
     * By default, [FileVisitor.preVisitDirectory] of the built file visitor returns [FileVisitResult.CONTINUE].
     *
     * @see FileVisitor.preVisitDirectory
     * @see FileVisitResult
     */
    public fun onPreVisitDirectory(function: (directory: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built [FileVisitor] with the provided [function].
     *
     * The provided callback is invoked when a file is visited.
     * Depending on the return value of the function:
     *   * [FileVisitResult.CONTINUE] or [FileVisitResult.SKIP_SUBTREE] - Traversal continues to a sibling entry.
     *   * [FileVisitResult.SKIP_SIBLINGS] - Sibling entries of the file are not visited.
     *   * [FileVisitResult.TERMINATE] - The traversal terminates immediately, and no further entries are visited.
     *
     * By default, [FileVisitor.visitFile] of the built file visitor returns [FileVisitResult.CONTINUE].
     *
     * @see FileVisitor.visitFile
     * @see FileVisitResult
     */
    public fun onVisitFile(function: (file: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built [FileVisitor] with the provided [function].
     *
     * The provided callback is invoked for an entry that could not be visited for some reason.
     * For example, when the entry's attributes could not be read, or the entry is a directory that could not be opened.
     * Depending on the return value of the function:
     *   * [FileVisitResult.CONTINUE] or [FileVisitResult.SKIP_SUBTREE] - Traversal continues to a sibling entry.
     *   * [FileVisitResult.SKIP_SIBLINGS] - Sibling entries are not visited.
     *   * [FileVisitResult.TERMINATE] - The traversal terminates immediately, and no further entries are visited.
     *
     * By default, [FileVisitor.visitFileFailed] of the built file visitor re-throws the I/O exception
     * that prevented the entry from being visited.
     *
     * @see FileVisitor.visitFileFailed
     * @see FileVisitResult
     */
    public fun onVisitFileFailed(function: (file: Path, exception: IOException) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built [FileVisitor] with the provided [function].
     *
     * The provided callback is invoked for a directory after its contents have been visited,
     * or when the iteration of the directory's immediate children is completed prematurely due to
     * an I/O exception or [onVisitFile] returning [FileVisitResult.SKIP_SIBLINGS].
     * Depending on the return value of the function:
     *   * [FileVisitResult.TERMINATE] - The traversal terminates immediately, and no further entries are visited.
     *   * Any other result - Traversal continues to a sibling entry. Returning [FileVisitResult.SKIP_SIBLINGS]
     *     or [FileVisitResult.SKIP_SUBTREE] has the same effect as [FileVisitResult.CONTINUE].
     *
     * Note that the callback is not invoked for directories whose contents were not visited.
     *
     * By default, if the directory iteration completes without an I/O exception,
     * [FileVisitor.postVisitDirectory] of the built file visitor returns [FileVisitResult.CONTINUE];
     * otherwise, it re-throws the I/O exception that caused the iteration to complete prematurely.
     *
     * @see FileVisitor.postVisitDirectory
     * @see FileVisitResult
     */
    public fun onPostVisitDirectory(function: (directory: Path, exception: IOException?) -> FileVisitResult): Unit
}


internal class FileVisitorBuilderImpl : FileVisitorBuilder {
    private var onPreVisitDirectory: ((Path, BasicFileAttributes) -> FileVisitResult)? = null
    private var onVisitFile: ((Path, BasicFileAttributes) -> FileVisitResult)? = null
    private var onVisitFileFailed: ((Path, IOException) -> FileVisitResult)? = null
    private var onPostVisitDirectory: ((Path, IOException?) -> FileVisitResult)? = null
    private var isBuilt: Boolean = false

    override fun onPreVisitDirectory(function: (directory: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit {
        checkIsNotBuilt()
        checkNotDefined(onPreVisitDirectory, "onPreVisitDirectory")
        onPreVisitDirectory = function
    }

    override fun onVisitFile(function: (file: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit {
        checkIsNotBuilt()
        checkNotDefined(onVisitFile, "onVisitFile")
        onVisitFile = function
    }

    override fun onVisitFileFailed(function: (file: Path, exception: IOException) -> FileVisitResult): Unit {
        checkIsNotBuilt()
        checkNotDefined(onVisitFileFailed, "onVisitFileFailed")
        onVisitFileFailed = function
    }

    override fun onPostVisitDirectory(function: (directory: Path, exception: IOException?) -> FileVisitResult): Unit {
        checkIsNotBuilt()
        checkNotDefined(onPostVisitDirectory, "onPostVisitDirectory")
        onPostVisitDirectory = function
    }

    fun build(): FileVisitor<Path> {
        checkIsNotBuilt()
        isBuilt = true
        return FileVisitorImpl(onPreVisitDirectory, onVisitFile, onVisitFileFailed, onPostVisitDirectory)
    }

    private fun checkIsNotBuilt() {
        if (isBuilt) throw IllegalStateException("This builder was already built")
    }

    private fun checkNotDefined(function: Any?, name: String) {
        if (function != null) throw IllegalStateException("$name was already defined")
    }
}


private class FileVisitorImpl(
    private val onPreVisitDirectory: ((Path, BasicFileAttributes) -> FileVisitResult)?,
    private val onVisitFile: ((Path, BasicFileAttributes) -> FileVisitResult)?,
    private val onVisitFileFailed: ((Path, IOException) -> FileVisitResult)?,
    private val onPostVisitDirectory: ((Path, IOException?) -> FileVisitResult)?,
) : SimpleFileVisitor<Path>() {
    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
        this.onPreVisitDirectory?.invoke(dir, attrs) ?: super.preVisitDirectory(dir, attrs)

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
        this.onVisitFile?.invoke(file, attrs) ?: super.visitFile(file, attrs)

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
        this.onVisitFileFailed?.invoke(file, exc) ?: super.visitFileFailed(file, exc)

    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult =
        this.onPostVisitDirectory?.invoke(dir, exc) ?: super.postVisitDirectory(dir, exc)
}
