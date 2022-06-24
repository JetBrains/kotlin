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
 * The builder to provide implementation of the file visitor that [fileVisitor] builds.
 */
@ExperimentalPathApi
@SinceKotlin("1.7")
public sealed interface FileVisitorBuilder {
    /**
     * Overrides the corresponding function of the built file visitor with the provided [function].
     *
     * By default, [FileVisitor.preVisitDirectory] of the built file visitor returns [FileVisitResult.CONTINUE].
     */
    public fun onPreVisitDirectory(function: (directory: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built file visitor with the provided [function].
     *
     * By default, [FileVisitor.visitFile] of the built file visitor returns [FileVisitResult.CONTINUE].
     */
    public fun onVisitFile(function: (file: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built file visitor with the provided [function].
     *
     * By default, [FileVisitor.visitFileFailed] of the built file visitor re-throws the I/O exception
     * that prevented the file from being visited.
     */
    public fun onVisitFileFailed(function: (file: Path, exception: IOException) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built file visitor with the provided [function].
     *
     * By default, if the directory iteration completes without an I/O exception,
     * [FileVisitor.postVisitDirectory] of the built file visitor returns [FileVisitResult.CONTINUE];
     * otherwise it re-throws the I/O exception that caused the iteration of the directory to terminate prematurely.
     */
    public fun onPostVisitDirectory(function: (directory: Path, exception: IOException?) -> FileVisitResult): Unit
}


@ExperimentalPathApi
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
