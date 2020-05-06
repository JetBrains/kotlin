/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "RedundantVisibilityModifier")
@file:JvmMultifileClass
@file:JvmName("PathsKt")
@file:kotlin.jvm.JvmPackageName("kotlin.io.jdk7")

package kotlin.io

import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException

/**
 * Returns the extension of this file (not including the dot), or an empty string if it doesn't have one.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public val Path.extension: String
    get() = fileName.toString().substringAfterLast('.', "")

/**
 * Returns [path][File.path] of this File using the invariant separator '/' to
 * separate the names in the name sequence.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public val Path.invariantSeparatorsPath: String
    get() {
        val separator = fileSystem.separator
        return if (separator != "/") toString().replace(separator, "/") else toString()
    }

/**
 * Returns file's name without an extension.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public val Path.nameWithoutExtension: String
    get() = fileName.toString().substringBeforeLast(".")


/**
 * Copies this path to the given [target] path.
 *
 * If some directories on a way to the [target] are missing, then they will be created.
 * If the [target] path already exists, this function will fail unless [overwrite] argument is set to `true`.
 *
 * When [overwrite] is `true` and [target] is a directory, it is replaced only if it is empty.
 *
 * If this file is a directory, it is copied without its content, i.e. an empty [target] directory is created.
 * If you want to copy directory including its contents, use [copyRecursively].
 *
 * The operation doesn't preserve copied file attributes such as creation/modification date, permissions, etc.
 *
 * @param overwrite `true` if destination overwrite is allowed.
 * @return the [target] file.
 * @throws NoSuchFileException if the source file doesn't exist.
 * @throws FileAlreadyExistsException if the destination file already exists and [overwrite] argument is set to `false`.
 * @throws IOException if any errors occur while copying.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public fun Path.copyTo(target: Path, overwrite: Boolean = false): Path {
    val options = if (overwrite) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
    return copyTo(target, *options)
}

/**
 * Copies this path to the given [target] path.
 *
 * If some directories on a way to the [target] are missing, then they will be created.
 * If the [target] path already exists, this function will fail unless the
 * [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is option is used.
 *
 * When [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is used and [target] is a directory,
 * it is replaced only if it is empty.
 *
 * If this file is a directory, it is copied without its content, i.e. an empty [target] directory is created.
 * If you want to copy directory including its contents, use [copyRecursively].
 *
 * The operation doesn't preserve copied file attributes such as creation/modification date,
 * permissions, etc. unless [COPY_ATTRIBUTES][StandardCopyOption.COPY_ATTRIBUTES] is used.
 *
 * @param options options to control how the path is copied.
 * @return the [target] file.
 * @throws NoSuchFileException if the source file doesn't exist.
 * @throws FileAlreadyExistsException if the destination file already exists and [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is not used.
 * @throws IOException if any errors occur while copying.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public fun Path.copyTo(target: Path, vararg options: CopyOption): Path {
    if (!this.exists()) {
        throw NoSuchFileException(toString(), null, "The source file doesn't exist.")
    }

    if (target.exists() && StandardCopyOption.REPLACE_EXISTING !in options) {
        throw FileAlreadyExistsException(toString(), null, "The destination file already exists.")
    }

    if (this.isDirectory()) {
        if (target.isDirectory() && Files.newDirectoryStream(target).use { it.firstOrNull() } != null) {
            throw FileAlreadyExistsException(toString(), null, "The destination file already exists.")
        }
        try {
            Files.createDirectories(target)
        } catch (_: FileAlreadyExistsException) {
            // File already exists and is not a directory
            Files.delete(target)
            Files.createDirectories(target)
        }
    } else {
        target.parent?.let { Files.createDirectories(it) }
        Files.copy(this, target, *options)
    }

    return target
}

/** Private exception class, used to terminate recursive copying. */
internal class TerminateException : IOException()

/**
 * Copies this file with all its children to the specified destination [target] path.
 * If some directories on the way to the destination are missing, then they will be created.
 *
 * If this file path points to a single file, then it will be copied to a file with the path [target].
 * If this file path points to a directory, then its children will be copied to a directory with the path [target].
 *
 * If the [target] already exists, it will be deleted before copying when the [overwrite] parameter permits so.
 *
 * The operation doesn't preserve copied file attributes such as creation/modification date, permissions, etc.
 *
 * If any errors occur during the copying, then further actions will depend on the result of the call
 * to `onError(File, IOException)` function, that will be called with arguments,
 * specifying the file that caused the error and the exception itself.
 * By default this function rethrows exceptions.
 *
 * Exceptions that can be passed to the `onError` function:
 *
 * - [NoSuchFileException] - if there was an attempt to copy a non-existent file
 * - [FileAlreadyExistsException] - if there is a conflict
 * - [AccessDeniedException] - if there was an attempt to open a directory that didn't succeed.
 * - [IOException] - if some problems occur when copying.
 *
 * Note that if this function fails, then partial copying may have taken place.
 *
 * @param overwrite `true` if it is allowed to overwrite existing destination files and directories.
 * @return `false` if the copying was terminated, `true` otherwise.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public fun Path.copyRecursively(
    target: Path,
    overwrite: Boolean = false,
    onError: (Path, IOException) -> OnErrorAction = { _, exception -> throw exception }
): Boolean {
    if (!this.exists()) {
        return onError(this, NoSuchFileException(toString(), null, "The source file doesn't exist.")) !=
                OnErrorAction.TERMINATE
    }
    try {
        // We cannot break for loop from inside a lambda, so we have to use an exception here
        for (src in walkTopDown().onFail { f, e -> if (onError(f, e) == OnErrorAction.TERMINATE) throw TerminateException() }) {
            if (!src.exists()) {
                if (onError(src, NoSuchFileException(src.toString(), null, "The source file doesn't exist.")) == OnErrorAction.TERMINATE)
                    return false
            } else {
                val dstFile = target.resolve(relativize(src))
                if (dstFile.exists() && !(src.isDirectory() && dstFile.isDirectory())) {
                    val stillExists = if (!overwrite) true else {
                        if (dstFile.isDirectory())
                            !dstFile.deleteRecursively()
                        else {
                            try {
                                Files.deleteIfExists(dstFile)
                                Files.exists(dstFile)
                            } catch (e: IOException) {
                                if (onError(dstFile, e) == OnErrorAction.TERMINATE) return false
                                continue
                            }
                        }
                    }

                    if (stillExists) {
                        if (onError(
                                dstFile, FileAlreadyExistsException(
                                    src.toString(),
                                    dstFile.toString(),
                                    "The destination file already exists."
                                )
                            ) == OnErrorAction.TERMINATE
                        ) {
                            return false
                        }

                        continue
                    }
                }

                if (src.isDirectory()) {
                    try {
                        Files.createDirectories(dstFile)
                    } catch (e: IOException) {
                        // handled in the next iteration
                    }
                } else {
                    val copy = try {
                        src.copyTo(dstFile, overwrite)
                    } catch (e: IOException) {
                        if (onError(src, e) == OnErrorAction.TERMINATE) {
                            return false
                        }
                        continue
                    }

                    fun fileLength(p: Path): Long {
                        return try {
                            Files.size(p)
                        } catch (e: IOException) {
                            0L
                        }
                    }

                    if (fileLength(copy) != fileLength(src)) {
                        if (onError(
                                src,
                                IOException("Source file wasn't copied completely, length of destination file differs.")
                            ) == OnErrorAction.TERMINATE
                        ) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    } catch (e: TerminateException) {
        return false
    }
}

/**
 * Delete this file with all its children.
 * Note that if this operation fails then partial deletion may have taken place.
 *
 * @return `true` if the file or directory is successfully deleted, `false` otherwise.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public fun Path.deleteRecursively(): Boolean = walkBottomUp().fold(true, { res, it ->
    try {
        Files.delete(it)
        true
    } catch (e: NoSuchFileException) {
        true
    } catch (e: IOException) {
        false
    } && res
})

/**
 * Check if this file exists.
 *
 * @param options Options to control how symbolic links are handled.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.exists(vararg options: LinkOption): Boolean = Files.exists(this, *options)

/**
 * Check if this path is a file.
 *
 * @param options Options to control how symbolic links are handled.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isFile(vararg options: LinkOption): Boolean = Files.isRegularFile(this, *options)

/**
 * Check if this path is a directory.
 *
 * By default, symbolic links in the path are followed.
 *
 * @param options Options to control how symbolic links are handled.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isDirectory(vararg options: LinkOption): Boolean = Files.isDirectory(this, *options)

/**
 * Check if this path exists and is a symbolic link.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isSymbolicLink(): Boolean = Files.isSymbolicLink(this)

/**
 * Check if this path exists and is executable.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isExecutable(): Boolean = Files.isExecutable(this)

/**
 * Check if this path is considered hidden.
 *
 * This check is dependant on the current filesystem. For example, on UNIX-like operating systems, a
 * path is considered hidden if its name begins with a dot. On Windows, file attributes are checked.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isHidden(): Boolean = Files.isHidden(this)

/**
 * Check if this path exists and is readable.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isReadable(): Boolean = Files.isReadable(this)

/**
 * Check that this path exists and is writable.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isWritable(): Boolean = Files.isWritable(this)

/**
 * Check if this path points to the same file or directory as [other].
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isSameFile(other: Path): Boolean = Files.isSameFile(this, other)

/**
 * Return a list of the files and directories in this directory.
 *
 * @throws NotDirectoryException If this path does not refer to a directory
 * @throws IOException If an I/O error occurs
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public fun Path.listFiles(): List<Path> {
    return Files.newDirectoryStream(this).use { it.toList() }
}
