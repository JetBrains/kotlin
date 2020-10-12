/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "RedundantVisibilityModifier")
@file:JvmMultifileClass
@file:JvmName("PathsKt")

package kotlin.io.path

import java.io.IOException
import java.nio.file.*
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException

/**
 * Returns the extension of this path (not including the dot), or an empty string if it doesn't have one.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public val Path.extension: String
    get() = fileName?.toString()?.substringAfterLast('.', "") ?: ""

/**
 * Returns this path as a [String] using the invariant separator '/' to
 * separate the names in the name sequence.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public val Path.invariantSeparatorsPath: String
    get() {
        val separator = fileSystem.separator
        return if (separator != "/") toString().replace(separator, "/") else toString()
    }

/**
 * Returns this path's [fileName][Path.getFileName] without an extension, or an empty string if
 * this path has zero elements.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public val Path.nameWithoutExtension: String
    get() = fileName?.toString()?.substringBeforeLast(".") ?: ""

/**
 * Calculates the relative path for this path from a [base] path.
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return Path with relative path from [base] to this.
 *
 * @throws IllegalArgumentException if this and base paths have different roots.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.relativeTo(base: Path): Path = try {
    PathRelativizer.tryRelativeTo(this, base)
} catch (e: IllegalArgumentException) {
    throw java.lang.IllegalArgumentException(e.message + "\nthis path: $this\nbase path: $base", e)
}

/**
 * Calculates the relative path for this path from a [base] path.
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return Path with relative path from [base] to this, or `this` if this and base paths have different roots.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.relativeToOrSelf(base: Path): Path =
    relativeToOrNull(base) ?: this

/**
 * Calculates the relative path for this path from a [base] path.
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return Path with relative path from [base] to this, or `null` if this and base paths have different roots.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.relativeToOrNull(base: Path): Path? = try {
    PathRelativizer.tryRelativeTo(this, base)
} catch (e: IllegalArgumentException) {
    null
}

internal object PathRelativizer {
    private val emptyPath = Paths.get("")
    private val parentPath = Paths.get("..")

    // Workarounds some bugs in Path.relativize that were fixed only in JDK9
    fun tryRelativeTo(path: Path, base: Path): Path {
        val bn = base.normalize()
        val pn = path.normalize()
        val rn = bn.relativize(pn)
        // work around https://bugs.openjdk.java.net/browse/JDK-8066943
        for (i in 0 until minOf(bn.nameCount, pn.nameCount)) {
            if (bn.getName(i) != parentPath) break
            if (pn.getName(i) != parentPath) throw IllegalArgumentException("Unable to compute relative path")
        }
        // work around https://bugs.openjdk.java.net/browse/JDK-8072495
        val r = if (pn != bn && bn == emptyPath) {
            pn
        } else {
            val rnString = rn.toString()
            // drop invalid dangling separator from path string https://bugs.openjdk.java.net/browse/JDK-8140449
            if (rnString.endsWith(rn.fileSystem.separator))
                rn.fileSystem.getPath(rnString.dropLast(rn.fileSystem.separator.length))
            else
                rn
        }
        return r
    }
}

/**
 * Copies this path to the given [target] path.
 *
 * Unlike `File.copyTo`, if some directories on a way to the [target] are missing, then they won't be created automatically.
 * You can use the following approach to ensure that required intermediate directories are created:
 * ```
 * sourcePath.copyTo(destinationPath.apply { parent?.createDirectories() })
 * ```
 *
 * If the [target] path already exists, this function will fail unless [overwrite] argument is set to `true`.
 *
 * When [overwrite] is `true` and [target] is a directory, it is replaced only if it is empty.
 *
 * If this path is a directory, it is copied without its content, i.e. an empty [target] directory is created.
 * If you want to copy directory including its contents, use [copyRecursively].
 *
 * The operation doesn't preserve copied file attributes such as creation/modification date, permissions, etc.
 *
 * @param overwrite `true` if destination overwrite is allowed.
 * @return the [target] path.
 * @throws NoSuchFileException if the source path doesn't exist.
 * @throws FileAlreadyExistsException if the destination path already exists and [overwrite] argument is set to `false`.
 * @throws DirectoryNotEmptyException if the destination path point to an existing directory and [overwrite] argument is `true`,
 *   when the directory being replaced is not empty.
 * @throws IOException if any errors occur while copying.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.copyTo(target: Path, overwrite: Boolean = false): Path {
    val options = if (overwrite) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
    return copyTo(target, *options)
}

/**
 * Copies this path to the given [target] path.
 *
 * Unlike `File.copyTo`, if some directories on a way to the [target] are missing, then they won't be created automatically.
 * You can use the following approach to ensure that required intermediate directories are created:
 * ```
 * sourcePath.copyTo(destinationPath.apply { parent?.createDirectories() })
 * ```
 *
 * If the [target] path already exists, this function will fail unless the
 * [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is option is used.
 *
 * When [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is used and [target] is a directory,
 * it is replaced only if it is empty.
 *
 * If this path is a directory, it is copied *without* its content, i.e. an empty [target] directory is created.
 * If you want to copy a directory including its contents, use [copyRecursively].
 *
 * The operation doesn't preserve copied file attributes such as creation/modification date,
 * permissions, etc. unless [COPY_ATTRIBUTES][StandardCopyOption.COPY_ATTRIBUTES] is used.
 *
 * @param options options to control how the path is copied.
 * @return the [target] path.
 * @throws NoSuchFileException if the source path doesn't exist.
 * @throws FileAlreadyExistsException if the destination path already exists and [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is not used.
 * @throws DirectoryNotEmptyException if the destination path point to an existing directory and [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is used,
 *   when the directory being replaced is not empty.
 * @throws IOException if any errors occur while copying.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.copyTo(target: Path, vararg options: CopyOption): Path {
    return Files.copy(this, target, *options)
}

/**
 * Check if this path exists.
 *
 * @param options Options to control how symbolic links are handled.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.exists(vararg options: LinkOption): Boolean = Files.exists(this, *options)

/**
 * Check if this path does not exist.
 *
 * @param options Options to control how symbolic links are handled.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.notExists(vararg options: LinkOption): Boolean = Files.notExists(this, *options)

/**
 * Check if this path is a file.
 *
 * @param options Options to control how symbolic links are handled.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isRegularFile(vararg options: LinkOption): Boolean = Files.isRegularFile(this, *options)

/**
 * Check if this path is a directory.
 *
 * By default, symbolic links in the path are followed.
 *
 * @param options Options to control how symbolic links are handled.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isDirectory(vararg options: LinkOption): Boolean = Files.isDirectory(this, *options)

/**
 * Check if this path exists and is a symbolic link.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isSymbolicLink(): Boolean = Files.isSymbolicLink(this)

/**
 * Check if this path exists and is executable.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isExecutable(): Boolean = Files.isExecutable(this)

/**
 * Check if this path is considered hidden.
 *
 * This check is dependant on the current filesystem. For example, on UNIX-like operating systems, a
 * path is considered hidden if its name begins with a dot. On Windows, file attributes are checked.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isHidden(): Boolean = Files.isHidden(this)

/**
 * Check if this path exists and is readable.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isReadable(): Boolean = Files.isReadable(this)

/**
 * Check that this path exists and is writable.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isWritable(): Boolean = Files.isWritable(this)

/**
 * Check if this path points to the same file or directory as [other].
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.isSameFile(other: Path): Boolean = Files.isSameFile(this, other)

/**
 * Return a list of the entries in this directory.
 *
 * @throws NotDirectoryException If this path does not refer to a directory
 * @throws IOException If an I/O error occurs
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.listDirectoryEntries(): List<Path> {
    return Files.newDirectoryStream(this).use { it.toList() }
}

/**
 * Call the [block] callback with a sequence of all entries in this directory.
 *
 * @throws NotDirectoryException If this path does not refer to a directory
 * @throws IOException If an I/O error occurs
 * @return the value returned by [block]
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun <T> Path.useDirectoryEntries(block: (Sequence<Path>) -> T): T {
    return Files.newDirectoryStream(this).use { block(it.asSequence()) }
}

/**
 * Perform the given [action] on each entry in this directory.
 *
 * @throws NotDirectoryException If this path does not refer to a directory
 * @throws IOException If an I/O error occurs
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.forEachDirectoryEntry(action: (Path) -> Unit) {
    return Files.newDirectoryStream(this).use { it.forEach(action) }
}
