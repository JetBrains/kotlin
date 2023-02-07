/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("PathsKt")

package kotlin.io.path

import java.io.IOException
import java.nio.file.*
import java.nio.file.FileSystemException
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes

/**
 * Recursively copies this directory and its content to the specified destination [target] path.
 * Note that if this function throws, partial copying may have taken place.
 *
 * Unlike `File.copyRecursively`, if some directories on the way to the [target] are missing, then they won't be created automatically.
 * You can use the following approach to ensure that required intermediate directories are created:
 * ```
 * sourcePath.copyToRecursively(
 *     destinationPath.apply { parent?.createDirectories() },
 *     followLinks = false
 * )
 * ```
 *
 * If the entry located by this path is a directory, this function recursively copies the directory itself and its content.
 * Otherwise, this function copies only the entry.
 *
 * If an exception occurs attempting to read, open or copy any entry under the source subtree,
 * further actions will depend on the result of the [onError] invoked with
 * the source and destination paths, that caused the error, and the exception itself as arguments.
 * If [onError] throws, this function ends immediately with the exception.
 * By default [onError] rethrows the exception. See [OnErrorResult] for available options.
 *
 * This function performs "directory merge" operation. If an entry in the source subtree is a directory
 * and the corresponding entry in the target subtree already exists and is also a directory, it does nothing.
 * Otherwise, [overwrite] determines whether to overwrite existing destination entries.
 * Attributes of a source entry, such as creation/modification date, are not copied.
 * [followLinks] determines whether to copy a symbolic link itself or its target.
 * Symbolic links in the target subtree are not followed.
 *
 * To provide a custom logic for copying use the overload that takes a `copyAction` lambda.
 *
 * @param target the destination path to copy recursively this entry to.
 * @param onError the function that determines further actions if an error occurs. By default, rethrows the exception.
 * @param followLinks `false` to copy a symbolic link itself, not its target.
 *   `true` to recursively copy the target of a symbolic link.
 * @param overwrite `false` to throw if a destination entry already exists.
 *   `true` to overwrite existing destination entries.
 * @throws NoSuchFileException if the entry located by this path does not exist.
 * @throws FileSystemException if [target] is an entry inside the source subtree.
 * @throws FileAlreadyExistsException if a destination entry already exists and [overwrite] is `false`.
 *   This exception is passed to [onError] for handling.
 * @throws IOException if any errors occur while copying.
 *   This exception is passed to [onError] for handling.
 * @throws SecurityException if a security manager is installed and access is not permitted to an entry in the source or target subtree.
 *   This exception is passed to [onError] for handling.
 */
@ExperimentalPathApi
@SinceKotlin("1.8")
public fun Path.copyToRecursively(
    target: Path,
    onError: (source: Path, target: Path, exception: Exception) -> OnErrorResult = { _, _, exception -> throw exception },
    followLinks: Boolean,
    overwrite: Boolean
): Path {
    return if (overwrite) {
        copyToRecursively(target, onError, followLinks) { src, dst ->
            val options = LinkFollowing.toLinkOptions(followLinks)
            val dstIsDirectory = dst.isDirectory(LinkOption.NOFOLLOW_LINKS)
            val srcIsDirectory = src.isDirectory(*options)
            if ((srcIsDirectory && dstIsDirectory).not()) {
                if (dstIsDirectory)
                    dst.deleteRecursively()

                src.copyTo(dst, *options, StandardCopyOption.REPLACE_EXISTING)
            }

            // else: do nothing, the destination directory already exists
            CopyActionResult.CONTINUE
        }
    } else {
        copyToRecursively(target, onError, followLinks)
    }
}

/**
 * Recursively copies this directory and its content to the specified destination [target] path.
 * Note that if this function throws, partial copying may have taken place.
 *
 * Unlike `File.copyRecursively`, if some directories on the way to the [target] are missing, then they won't be created automatically.
 * You can use the following approach to ensure that required intermediate directories are created:
 * ```
 * sourcePath.copyToRecursively(
 *     destinationPath.apply { parent?.createDirectories() },
 *     followLinks = false
 * )
 * ```
 *
 * If the entry located by this path is a directory, this function recursively copies the directory itself and its content.
 * Otherwise, this function copies only the entry.
 *
 * If an exception occurs attempting to read, open or copy any entry under the source subtree,
 * further actions will depend on the result of the [onError] invoked with
 * the source and destination paths, that caused the error, and the exception itself as arguments.
 * If [onError] throws, this function ends immediately with the exception.
 * By default [onError] rethrows the exception. See [OnErrorResult] for available options.
 *
 * Copy operation is performed using [copyAction].
 * By default [copyAction] performs "directory merge" operation. If an entry in the source subtree is a directory
 * and the corresponding entry in the target subtree already exists and is also a directory, it does nothing.
 * Otherwise, the entry is copied using `sourcePath.copyTo(destinationPath, *followLinksOption)`,
 * which doesn't copy attributes of the source entry and throws if the destination entry already exists.
 * [followLinks] determines whether to copy a symbolic link itself or its target.
 * Symbolic links in the target subtree are not followed.
 *
 * If a custom implementation of [copyAction] is provided, consider making it consistent with [followLinks] value.
 * See [CopyActionResult] for available options.
 *
 * If [copyAction] throws an exception, it is passed to [onError] for handling.
 *
 * @param target the destination path to copy recursively this entry to.
 * @param onError the function that determines further actions if an error occurs. By default, rethrows the exception.
 * @param followLinks `false` to copy a symbolic link itself, not its target.
 *   `true` to recursively copy the target of a symbolic link.
 * @param copyAction the function to call for copying source entries to their destination path rooted in [target].
 *   By default, performs "directory merge" operation.
 * @throws NoSuchFileException if the entry located by this path does not exist.
 * @throws FileSystemException if [target] is an entry inside the source subtree.
 * @throws IOException if any errors occur while copying.
 *   This exception is passed to [onError] for handling.
 * @throws SecurityException if a security manager is installed and access is not permitted to an entry in the source or target subtree.
 *   This exception is passed to [onError] for handling.
 */
@ExperimentalPathApi
@SinceKotlin("1.8")
public fun Path.copyToRecursively(
    target: Path,
    onError: (source: Path, target: Path, exception: Exception) -> OnErrorResult = { _, _, exception -> throw exception },
    followLinks: Boolean,
    copyAction: CopyActionContext.(source: Path, target: Path) -> CopyActionResult = { src, dst ->
        src.copyToIgnoringExistingDirectory(dst, followLinks)
    }
): Path {
    if (!this.exists(*LinkFollowing.toLinkOptions(followLinks)))
        throw NoSuchFileException(this.toString(), target.toString(), "The source file doesn't exist.")

    if (this.exists() && (followLinks || !this.isSymbolicLink())) {
        // Here the checks are conducted without followLinks option, because:
        //   * isSameFileAs doesn't take LinkOption and throws if `this` or `other` file doesn't exist
        //   * toRealPath takes LinkOption, but the option also applies to the directories on the way to the file
        // Thus links are always followed in isSameFileAs and toRealPath

        val targetExistsAndNotSymlink = target.exists() && !target.isSymbolicLink()

        if (targetExistsAndNotSymlink && this.isSameFileAs(target)) {
            // TODO: KT-38678
            // source and target files are the same entry, continue recursive copy operation
        } else {
            val isSubdirectory = when {
                this.fileSystem != target.fileSystem ->
                    false
                targetExistsAndNotSymlink ->
                    target.toRealPath().startsWith(this.toRealPath())
                else ->
                    target.parent?.let { it.exists() && it.toRealPath().startsWith(this.toRealPath()) } ?: false
            }
            if (isSubdirectory)
                throw FileSystemException(
                    this.toString(),
                    target.toString(),
                    "Recursively copying a directory into its subdirectory is prohibited."
                )
        }
    }

    fun destination(source: Path): Path {
        val relativePath = source.relativeTo(this@copyToRecursively)
        return target.resolve(relativePath.pathString)
    }

    fun error(source: Path, exception: Exception): FileVisitResult {
        return onError(source, destination(source), exception).toFileVisitResult()
    }

    @Suppress("UNUSED_PARAMETER")
    fun copy(source: Path, attributes: BasicFileAttributes): FileVisitResult {
        return try {
            DefaultCopyActionContext.copyAction(source, destination(source)).toFileVisitResult()
        } catch (exception: Exception) {
            error(source, exception)
        }
    }

    visitFileTree(followLinks = followLinks) {
        onPreVisitDirectory(::copy)
        onVisitFile(::copy)
        onVisitFileFailed(::error)
        onPostVisitDirectory { directory, exception ->
            if (exception == null) {
                FileVisitResult.CONTINUE
            } else {
                error(directory, exception)
            }
        }
    }

    return target
}


@ExperimentalPathApi
private object DefaultCopyActionContext : CopyActionContext {
    override fun Path.copyToIgnoringExistingDirectory(target: Path, followLinks: Boolean): CopyActionResult {
        val options = LinkFollowing.toLinkOptions(followLinks)
        if (this.isDirectory(*options) && target.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
            // do nothing, the destination directory already exists
        } else {
            this.copyTo(target, *options)
        }

        return CopyActionResult.CONTINUE
    }
}

@ExperimentalPathApi
private fun CopyActionResult.toFileVisitResult() = when (this) {
    CopyActionResult.CONTINUE -> FileVisitResult.CONTINUE
    CopyActionResult.TERMINATE -> FileVisitResult.TERMINATE
    CopyActionResult.SKIP_SUBTREE -> FileVisitResult.SKIP_SUBTREE
}

@ExperimentalPathApi
private fun OnErrorResult.toFileVisitResult() = when (this) {
    OnErrorResult.TERMINATE -> FileVisitResult.TERMINATE
    OnErrorResult.SKIP_SUBTREE -> FileVisitResult.SKIP_SUBTREE
}

/**
 * Recursively deletes this directory and its content.
 * Note that if this function throws, partial deletion may have taken place.
 *
 * If the entry located by this path is a directory, this function recursively deletes its content and the directory itself.
 * Otherwise, this function deletes only the entry.
 * Symbolic links are not followed to their targets.
 * This function does nothing if the entry located by this path does not exist.
 *
 * If the underlying platform supports [SecureDirectoryStream],
 * traversal of the file tree and removal of entries are performed using it.
 * Otherwise, directories in the file tree are opened with the less secure [Files.newDirectoryStream].
 * Note that on a platform that supports symbolic links and does not support [SecureDirectoryStream],
 * it is possible for a recursive delete to delete files and directories that are outside the directory being deleted.
 * This can happen if, after checking that an entry is a directory (and not a symbolic link), that directory is replaced
 * by a symbolic link to an outside directory before the call that opens the directory to read its entries.
 *
 * If an exception occurs attempting to read, open or delete any entry under the given file tree,
 * this method skips that entry and continues. Such exceptions are collected and, after attempting to delete all entries,
 * an [IOException] is thrown containing those exceptions as suppressed exceptions.
 * Maximum of `64` exceptions are collected. After reaching that amount, thrown exceptions are ignored and not collected.
 *
 * @throws IOException if any entry in the file tree can't be deleted for any reason.
 */
@ExperimentalPathApi
@SinceKotlin("1.8")
public fun Path.deleteRecursively(): Unit {
    val suppressedExceptions = this.deleteRecursivelyImpl()

    if (suppressedExceptions.isNotEmpty()) {
        throw FileSystemException("Failed to delete one or more files. See suppressed exceptions for details.").apply {
            suppressedExceptions.forEach { addSuppressed(it) }
        }
    }
}

private class ExceptionsCollector(private val limit: Int = 64) {
    var totalExceptions: Int = 0
        private set

    val collectedExceptions = mutableListOf<Exception>()

    var path: Path? = null

    fun enterEntry(name: Path) {
        path = path?.resolve(name)
    }

    fun exitEntry(name: Path) {
        require(name == path?.fileName)
        path = path?.parent
    }

    fun collect(exception: Exception) {
        totalExceptions += 1
        val shouldCollect = collectedExceptions.size < limit
        if (shouldCollect) {
            val restoredException = if (path != null) {
                // When SecureDirectoryStream is used, only entry name gets reported in exception message.
                // Thus, wrap such exceptions in FileSystemException with restored path.
                FileSystemException(path.toString()).initCause(exception) as FileSystemException
            } else {
                exception
            }
            collectedExceptions.add(restoredException)
        }
    }
}

private fun Path.deleteRecursivelyImpl(): List<Exception> {
    val collector = ExceptionsCollector()
    var useInsecure = true

    // TODO: KT-54077
    this.parent?.let { parent ->
        val directoryStream = try { Files.newDirectoryStream(parent) } catch (_: Throwable) { null }
        directoryStream?.use { stream ->
            if (stream is SecureDirectoryStream<Path>) {
                useInsecure = false
                collector.path = parent
                stream.handleEntry(this.fileName, collector)
            }
        }
    }

    if (useInsecure) {
        insecureHandleEntry(this, collector)
    }

    return collector.collectedExceptions
}

private inline fun collectIfThrows(collector: ExceptionsCollector, function: () -> Unit) {
    try {
        function()
    } catch (exception: Exception) {
        collector.collect(exception)
    }
}

private inline fun <R> tryIgnoreNoSuchFileException(function: () -> R): R? {
    return try { function() } catch (_: NoSuchFileException) { null }
}

// secure walk

private fun SecureDirectoryStream<Path>.handleEntry(name: Path, collector: ExceptionsCollector) {
    collector.enterEntry(name)

    collectIfThrows(collector) {
        if (this.isDirectory(name, LinkOption.NOFOLLOW_LINKS)) {
            val preEnterTotalExceptions = collector.totalExceptions

            this.enterDirectory(name, collector)

            // If something went wrong trying to delete the contents of the
            // directory, don't try to delete the directory as it will probably fail.
            if (preEnterTotalExceptions == collector.totalExceptions) {
                tryIgnoreNoSuchFileException { this.deleteDirectory(name) }
            }
        } else {
            tryIgnoreNoSuchFileException { this.deleteFile(name) } // deletes symlink itself, not its target
        }
    }

    collector.exitEntry(name)
}

private fun SecureDirectoryStream<Path>.enterDirectory(name: Path, collector: ExceptionsCollector) {
    collectIfThrows(collector) {
        tryIgnoreNoSuchFileException {
            this.newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS)
        }?.use { directoryStream ->
            for (entry in directoryStream) {
                directoryStream.handleEntry(entry.fileName, collector)
            }
        }
    }
}

private fun SecureDirectoryStream<Path>.isDirectory(entryName: Path, vararg options: LinkOption): Boolean {
    return tryIgnoreNoSuchFileException {
        this.getFileAttributeView(entryName, BasicFileAttributeView::class.java, *options).readAttributes().isDirectory
    } ?: false
}

// insecure walk

private fun insecureHandleEntry(entry: Path, collector: ExceptionsCollector) {
    collectIfThrows(collector) {
        if (entry.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
            val preEnterTotalExceptions = collector.totalExceptions

            insecureEnterDirectory(entry, collector)

            // If something went wrong trying to delete the contents of the
            // directory, don't try to delete the directory as it will probably fail.
            if (preEnterTotalExceptions == collector.totalExceptions) {
                entry.deleteIfExists()
            }
        } else {
            entry.deleteIfExists() // deletes symlink itself, not its target
        }
    }
}

private fun insecureEnterDirectory(path: Path, collector: ExceptionsCollector) {
    collectIfThrows(collector) {
        tryIgnoreNoSuchFileException {
            Files.newDirectoryStream(path)
        }?.use { directoryStream ->
            for (entry in directoryStream) {
                insecureHandleEntry(entry, collector)
            }
        }
    }
}
