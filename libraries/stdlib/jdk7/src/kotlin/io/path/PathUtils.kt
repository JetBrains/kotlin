/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "RedundantVisibilityModifier")
@file:JvmMultifileClass
@file:JvmName("PathsKt")

package kotlin.io.path

import java.io.IOException
import java.net.URI
import java.nio.file.*
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.attribute.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.Throws

/**
 * Returns the name of the file or directory denoted by this path as a string,
 * or an empty string if this path has zero path elements.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
public val Path.name: String
    get() = fileName?.toString().orEmpty()

/**
 * Returns the [name][Path.name] of this file or directory without an extension,
 * or an empty string if this path has zero path elements.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
public val Path.nameWithoutExtension: String
    get() = fileName?.toString()?.substringBeforeLast(".") ?: ""

/**
 * Returns the extension of this path (not including the dot),
 * or an empty string if it doesn't have one.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
public val Path.extension: String
    get() = fileName?.toString()?.substringAfterLast('.', "") ?: ""

/**
 * Returns the string representation of this path.
 *
 * The returned path string uses the default name [separator][FileSystem.getSeparator]
 * to separate names in the path.
 *
 * This property is a synonym to [Path.toString] function.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline val Path.pathString: String
    get() = toString()

/**
 * Returns the string representation of this path using the invariant separator '/'
 * to separate names in the path.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
public val Path.invariantSeparatorsPathString: String
    get() {
        val separator = fileSystem.separator
        return if (separator != "/") toString().replace(separator, "/") else toString()
    }

@SinceKotlin("1.4")
@ExperimentalPathApi
@Deprecated("Use invariantSeparatorsPathString property instead.", ReplaceWith("invariantSeparatorsPathString"),
            level = DeprecationLevel.ERROR)
@kotlin.internal.InlineOnly
public inline val Path.invariantSeparatorsPath: String
    get() = invariantSeparatorsPathString

/**
 * Converts this possibly relative path to an absolute path.
 *
 * If this path is already [absolute][Path.isAbsolute], returns this path unchanged.
 * Otherwise, resolves this path, usually against the default directory of the file system.
 *
 * May throw an exception if the file system is inaccessible or getting the default directory path is prohibited.
 *
 * See [Path.toAbsolutePath] for further details about the function contract and possible exceptions.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.absolute(): Path = toAbsolutePath()

/**
 * Converts this possibly relative path to an absolute path and returns its string representation.
 *
 * Basically, this method is a combination of calling [absolute] and [pathString].
 *
 * May throw an exception if the file system is inaccessible or getting the default directory path is prohibited.
 *
 * See [Path.toAbsolutePath] for further details about the function contract and possible exceptions.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.absolutePathString(): String = toAbsolutePath().toString()

/**
 * Calculates the relative path for this path from a [base] path.
 *
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return the relative path from [base] to this.
 *
 * @throws IllegalArgumentException if this and base paths have different roots.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
public fun Path.relativeTo(base: Path): Path = try {
    PathRelativizer.tryRelativeTo(this, base)
} catch (e: IllegalArgumentException) {
    throw IllegalArgumentException(e.message + "\nthis path: $this\nbase path: $base", e)
}

/**
 * Calculates the relative path for this path from a [base] path.
 *
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return the relative path from [base] to this, or `this` if this and base paths have different roots.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
public fun Path.relativeToOrSelf(base: Path): Path =
    relativeToOrNull(base) ?: this

/**
 * Calculates the relative path for this path from a [base] path.
 *
 * Note that the [base] path is treated as a directory.
 * If this path matches the [base] path, then a [Path] with an empty path will be returned.
 *
 * @return the relative path from [base] to this, or `null` if this and base paths have different roots.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
public fun Path.relativeToOrNull(base: Path): Path? = try {
    PathRelativizer.tryRelativeTo(this, base)
} catch (e: IllegalArgumentException) {
    null
}

private object PathRelativizer {
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
 * Copies a file or directory located by this path to the given [target] path.
 *
 * Unlike `File.copyTo`, if some directories on the way to the [target] are missing, then they won't be created automatically.
 * You can use the [createParentDirectories] function to ensure that required intermediate directories are created:
 * ```
 * sourcePath.copyTo(destinationPath.createParentDirectories())
 * ```
 *
 * If the [target] path already exists, this function will fail unless [overwrite] argument is set to `true`.
 *
 * When [overwrite] is `true` and [target] is a directory, it is replaced only if it is empty.
 *
 * If this path is a directory, it is copied without its content, i.e. an empty [target] directory is created.
 * If you want to copy directory including its contents, use [copyToRecursively].
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
 *
 * @see Files.copy
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.copyTo(target: Path, overwrite: Boolean = false): Path {
    val options = if (overwrite) arrayOf<CopyOption>(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
    return Files.copy(this, target, *options)
}

/**
 * Copies a file or directory located by this path to the given [target] path.
 *
 * Unlike `File.copyTo`, if some directories on the way to the [target] are missing, then they won't be created automatically.
 * You can use the [createParentDirectories] function to ensure that required intermediate directories are created:
 * ```
 * sourcePath.copyTo(destinationPath.createParentDirectories())
 * ```
 *
 * If the [target] path already exists, this function will fail unless the
 * [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is option is used.
 *
 * When [REPLACE_EXISTING][StandardCopyOption.REPLACE_EXISTING] is used and [target] is a directory,
 * it is replaced only if it is empty.
 *
 * If this path is a directory, it is copied *without* its content, i.e. an empty [target] directory is created.
 * If you want to copy a directory including its contents, use [copyToRecursively].
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
 *
 * @see Files.copy
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.copyTo(target: Path, vararg options: CopyOption): Path {
    return Files.copy(this, target, *options)
}

/**
 * Checks if the file located by this path exists.
 *
 * @return `true`, if the file definitely exists, `false` otherwise,
 * including situations when the existence cannot be determined.
 *
 * @param options options to control how symbolic links are handled.
 *
 * @see Files.exists
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.exists(vararg options: LinkOption): Boolean = Files.exists(this, *options)

/**
 * Checks if the file located by this path does not exist.
 *
 * @return `true`, if the file definitely does not exist, `false` otherwise,
 * including situations when the existence cannot be determined.
 *
 * @param options options to control how symbolic links are handled.
 *
 * @see Files.notExists
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.notExists(vararg options: LinkOption): Boolean = Files.notExists(this, *options)

/**
 * Checks if the file located by this path is a regular file.
 *
 * @param options options to control how symbolic links are handled.
 *
 * @see Files.isRegularFile
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.isRegularFile(vararg options: LinkOption): Boolean = Files.isRegularFile(this, *options)

/**
 * Checks if the file located by this path is a directory.
 *
 * By default, symbolic links in the path are followed.
 *
 * @param options options to control how symbolic links are handled.
 *
 * @see Files.isDirectory
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.isDirectory(vararg options: LinkOption): Boolean = Files.isDirectory(this, *options)

/**
 * Checks if the file located by this path exists and is a symbolic link.
 *
 * @see Files.isSymbolicLink
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.isSymbolicLink(): Boolean = Files.isSymbolicLink(this)

/**
 * Checks if the file located by this path exists and is executable.
 *
 * @see Files.isExecutable
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.isExecutable(): Boolean = Files.isExecutable(this)

/**
 * Checks if the file located by this path is considered hidden.
 *
 * This check is dependant on the current filesystem. For example, on UNIX-like operating systems, a
 * path is considered hidden if its name begins with a dot. On Windows, file attributes are checked.
 *
 * @see Files.isHidden
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.isHidden(): Boolean = Files.isHidden(this)

/**
 * Checks if the file located by this path exists and is readable.
 *
 * @see Files.isReadable
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.isReadable(): Boolean = Files.isReadable(this)

/**
 * Checks if the file located by this path exists and is writable.
 *
 * @see Files.isWritable
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path.isWritable(): Boolean = Files.isWritable(this)

/**
 * Checks if the file located by this path points to the same file or directory as [other].
 *
 * @see Files.isSameFile
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.isSameFileAs(other: Path): Boolean = Files.isSameFile(this, other)

/**
 * Returns a list of the entries in this directory optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path does not refer to a directory.
 * @throws IOException If an I/O error occurs.
 *
 * @see Files.newDirectoryStream
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
public fun Path.listDirectoryEntries(glob: String = "*"): List<Path> {
    return Files.newDirectoryStream(this, glob).use { it.toList() }
}

/**
 * Calls the [block] callback with a sequence of all entries in this directory
 * optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path does not refer to a directory.
 * @throws IOException If an I/O error occurs.
 * @return the value returned by [block].
 *
 * @see Files.newDirectoryStream
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun <T> Path.useDirectoryEntries(glob: String = "*", block: (Sequence<Path>) -> T): T {
    return Files.newDirectoryStream(this, glob).use { block(it.asSequence()) }
}

/**
 * Performs the given [action] on each entry in this directory optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path does not refer to a directory.
 * @throws IOException If an I/O error occurs.
 *
 * @see Files.newDirectoryStream
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.forEachDirectoryEntry(glob: String = "*", action: (Path) -> Unit) {
    return Files.newDirectoryStream(this, glob).use { it.forEach(action) }
}

/**
 * Returns the size of a regular file as a [Long] value of bytes or throws an exception if the file doesn't exist.
 *
 * @throws IOException if an I/O error occurred.
 * @see Files.size
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.fileSize(): Long =
    Files.size(this)

/**
 * Deletes the existing file or empty directory specified by this path.
 *
 * @throws NoSuchFileException if the file or directory does not exist.
 * @throws DirectoryNotEmptyException if the directory exists but is not empty.
 *
 * @see Files.delete
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.deleteExisting() {
    Files.delete(this)
}

/**
 * Deletes the file or empty directory specified by this path if it exists.
 *
 * @return `true` if the existing file was successfully deleted, `false` if the file does not exist.
 *
 * @throws DirectoryNotEmptyException if the directory exists but is not empty
 *
 * @see Files.deleteIfExists
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.deleteIfExists() =
    Files.deleteIfExists(this)

/**
 * Creates a new directory or throws an exception if there is already a file or directory located by this path.
 *
 * Note that the parent directory where this directory is going to be created must already exist.
 * If you need to create all non-existent parent directories, use [Path.createDirectories].
 *
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 *
 * @throws FileAlreadyExistsException if there is already a file or directory located by this path
 * (optional specific exception, some implementations may throw more general [IOException]).
 * @throws IOException if an I/O error occurs or the parent directory does not exist.
 * @throws UnsupportedOperationException if the [attributes ]array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createDirectory
 * @see Path.createDirectories
 * @see Path.createParentDirectories
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.createDirectory(vararg attributes: FileAttribute<*>): Path =
    Files.createDirectory(this, *attributes)

/**
 * Creates a directory ensuring that all nonexistent parent directories exist by creating them first.
 *
 * If the directory already exists, this function does not throw an exception, unlike [Path.createDirectory].
 *
 * @return the path of this directory if it already exists or has been created successfully.
 * The returned path can be converted [Path.toAbsolutePath][to absolute path] if it was relative.
 *
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 *
 * @throws FileAlreadyExistsException if there is already a file located by this path or one of its parent paths
 * (optional specific exception, some implementations may throw more general [IOException]).
 * @throws IOException if an I/O error occurs.
 * @throws UnsupportedOperationException if the [attributes] array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createDirectories
 * @see Path.createDirectory
 * @see Path.createParentDirectories
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.createDirectories(vararg attributes: FileAttribute<*>): Path =
    Files.createDirectories(this, *attributes)

/**
 * Ensures that all parent directories of this path exist, creating them if required.
 *
 * If the parent directory already exists, this function does nothing.
 *
 * Note that the [parent][Path.getParent] directory is not always the directory that contains the entry specified by this path.
 * For example, the parent of the path `x/y/.` is `x/y`, which is logically the same directory,
 * and the parent of `x/y/..` (which means just `x/`) is also `x/y`.
 * Use the function [Path.normalize] to eliminate redundant name elements from the path.
 *
 * @param attributes an optional list of file attributes to set atomically when creating the missing parent directories.
 *
 * @return this path unchanged if all parent directories already exist or have been created successfully.
 *
 * @throws FileAlreadyExistsException if there is already a file located by the [parent][Path.getParent] path or one of its parent paths
 * (optional specific exception, some implementations may throw more general [IOException]).
 * @throws IOException if an I/O error occurs.
 * @throws UnsupportedOperationException if the [attributes] array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Path.getParent
 * @see Path.createDirectories
 */
@SinceKotlin("1.9")
@Throws(IOException::class)
public fun Path.createParentDirectories(vararg attributes: FileAttribute<*>): Path = also {
    val parent = it.parent
    if (parent != null && !parent.isDirectory()) {
        try {
            parent.createDirectories(*attributes)
        } catch (e: FileAlreadyExistsException) {
            if (!parent.isDirectory()) throw e
        }
    }
}

/**
 * Moves or renames the file located by this path to the [target] path.
 *
 * @param options options specifying how the move should be done, see [StandardCopyOption], [LinkOption].
 *
 * @throws FileAlreadyExistsException if the target file exists but cannot be replaced because the
 *   [StandardCopyOption.REPLACE_EXISTING] option is not specified (optional specific exception).
 * @throws DirectoryNotEmptyException the [StandardCopyOption.REPLACE_EXISTING] option is specified but the file
 *   cannot be replaced because it is a non-empty directory, or the
 *   source is a non-empty directory containing entries that would
 *   be required to be moved (optional specific exception).
 *
 * @see Files.move
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.moveTo(target: Path, vararg options: CopyOption): Path =
    Files.move(this, target, *options)

/**
 * Moves or renames the file located by this path to the [target] path.
 *
 * @param overwrite allows to overwrite the target if it already exists.
 *
 * @throws FileAlreadyExistsException if the target file exists but cannot be replaced because the
 *   `overwrite = true` option is not specified (optional specific exception).
 * @throws DirectoryNotEmptyException the `overwrite = true` option is specified but the file
 *   cannot be replaced because it is a non-empty directory, or the
 *   source is a non-empty directory containing entries that would
 *   be required to be moved (optional specific exception).
 *
 * @see Files.move
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.moveTo(target: Path, overwrite: Boolean = false): Path {
    val options = if (overwrite) arrayOf<CopyOption>(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
    return Files.move(this, target, *options)
}

/**
 * Returns the [FileStore] representing the file store where a file is located.
 *
 * @see Files.getFileStore
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.fileStore(): FileStore =
    Files.getFileStore(this)

/**
 * Reads the value of a file attribute.
 *
 * The attribute name is specified with the [attribute] parameter optionally prefixed with the attribute view name:
 * ```
 * [view_name:]attribute_name
 * ```
 * When the view name is not specified, it defaults to `basic`.
 *
 * @throws UnsupportedOperationException if the attribute view is not supported.
 * @throws IllegalArgumentException if the attribute name is not specified or is not recognized.
 * @see Files.getAttribute
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.getAttribute(attribute: String, vararg options: LinkOption): Any? =
    Files.getAttribute(this, attribute, *options)

/**
 * Sets the value of a file attribute.
 *
 * The attribute name is specified with the [attribute] parameter optionally prefixed with the attribute view name:
 * ```
 * [view_name:]attribute_name
 * ```
 * When the view name is not specified, it defaults to `basic`.
 *
 * @throws UnsupportedOperationException if the attribute view is not supported.
 * @throws IllegalArgumentException if the attribute name is not specified or is not recognized, or
 *   the attribute value is of the correct type but has an inappropriate value.
 * @throws ClassCastException if the attribute value is not of the expected type
 * @see Files.setAttribute
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.setAttribute(attribute: String, value: Any?, vararg options: LinkOption): Path =
    Files.setAttribute(this, attribute, value, *options)

/**
 * Returns a file attributes view of a given type [V]
 * or `null` if the requested attribute view type is not available.
 *
 * The returned view allows to read and optionally to modify attributes of a file.
 *
 * @param V the reified type of the desired attribute view.
 *
 * @see Files.getFileAttributeView
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun <reified V : FileAttributeView> Path.fileAttributesViewOrNull(vararg options: LinkOption): V? =
    Files.getFileAttributeView(this, V::class.java, *options)

/**
 * Returns a file attributes view of a given type [V]
 * or throws an [UnsupportedOperationException] if the requested attribute view type is not available..
 *
 * The returned view allows to read and optionally to modify attributes of a file.
 *
 * @param V the reified type of the desired attribute view, a subtype of [FileAttributeView].
 *
 * @see Files.getFileAttributeView
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun <reified V : FileAttributeView> Path.fileAttributesView(vararg options: LinkOption): V =
    Files.getFileAttributeView(this, V::class.java, *options) ?: fileAttributeViewNotAvailable(this, V::class.java)

@PublishedApi
internal fun fileAttributeViewNotAvailable(path: Path, attributeViewClass: Class<*>): Nothing =
    throw UnsupportedOperationException("The desired attribute view type $attributeViewClass is not available for the file $path.")

/**
 * Reads a file's attributes of the specified type [A] in bulk.
 *
 * @param A the reified type of the desired attributes, a subtype of [BasicFileAttributes].
 *
 * @throws UnsupportedOperationException if the given attributes type [A] is not supported.
 * @see Files.readAttributes
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun <reified A : BasicFileAttributes> Path.readAttributes(vararg options: LinkOption): A =
    Files.readAttributes(this, A::class.java, *options)

/**
 * Reads the specified list of attributes of a file in bulk.
 *
 * The list of [attributes] to read is specified in the following string form:
 * ```
 * [view:]attribute_name1[,attribute_name2...]
 * ```
 * So the names are comma-separated and optionally prefixed by the attribute view type name, `basic` by default.
 * The special `*` attribute name can be used to read all attributes of the specified view.
 *
 * @return a [Map<String, Any?>][Map] having an entry for an each attribute read, where the key is the attribute name and the value is the attribute value.
 * @throws UnsupportedOperationException if the attribute view is not supported.
 * @throws IllegalArgumentException if no attributes are specified or an unrecognized attribute is specified.
 * @see Files.readAttributes
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.readAttributes(attributes: String, vararg options: LinkOption): Map<String, Any?> =
    Files.readAttributes(this, attributes, *options)

/**
 * Returns the last modified time of the file located by this path.
 *
 * If the file system does not support modification timestamps, some implementation-specific default is returned.
 *
 * @see Files.getLastModifiedTime
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.getLastModifiedTime(vararg options: LinkOption): FileTime =
    Files.getLastModifiedTime(this, *options)

/**
 * Sets the last modified time attribute for the file located by this path.
 *
 * If the file system does not support modification timestamps, the behavior of this method is not defined.
 *
 * @see Files.setLastModifiedTime
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.setLastModifiedTime(value: FileTime): Path =
    Files.setLastModifiedTime(this, value)

/**
 * Returns the owner of a file.
 *
 * @throws UnsupportedOperationException if the associated file system does not support the [FileOwnerAttributeView].
 *
 * @see Files.getOwner
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.getOwner(vararg options: LinkOption): UserPrincipal? =
    Files.getOwner(this, *options)

/**
 * Sets the file owner to the specified [value].
 *
 * @throws UnsupportedOperationException if the associated file system does not support the [FileOwnerAttributeView].
 *
 * @see Files.setOwner
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.setOwner(value: UserPrincipal): Path =
    Files.setOwner(this, value)

/**
 * Returns the POSIX file permissions of the file located by this path.
 *
 * @throws UnsupportedOperationException if the associated file system does not support the [PosixFileAttributeView].
 *
 * @see Files.getPosixFilePermissions
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.getPosixFilePermissions(vararg options: LinkOption): Set<PosixFilePermission> =
    Files.getPosixFilePermissions(this, *options)

/**
 * Sets the POSIX file permissions for the file located by this path.
 *
 * @throws UnsupportedOperationException if the associated file system does not support the [PosixFileAttributeView].
 *
 * @see Files.setPosixFilePermissions
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.setPosixFilePermissions(value: Set<PosixFilePermission>): Path =
    Files.setPosixFilePermissions(this, value)

/**
 * Creates a new link (directory entry) located by this path for the existing file [target].
 *
 * Calling this function may require the process to be started with implementation specific privileges to create hard links
 * or to create links to directories.
 *
 * @throws FileAlreadyExistsException if a file with this name already exists
 *   (optional specific exception, some implementations may throw a more general one).
 * @throws  UnsupportedOperationException if the implementation does not support creating a hard link.
 *
 * @see Files.createLink
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.createLinkPointingTo(target: Path): Path =
    Files.createLink(this, target)

/**
 * Creates a new symbolic link located by this path to the given [target].
 *
 * Calling this function may require the process to be started with implementation specific privileges to
 * create symbolic links.
 *
 * @throws FileAlreadyExistsException if a file with this name already exists
 *   (optional specific exception, some implementations may throw a more general one).
 * @throws UnsupportedOperationException if the implementation does not support symbolic links or the
 *   [attributes] array contains an attribute that cannot be set atomically when creating the symbolic link.
 *
 * @see Files.createSymbolicLink
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.createSymbolicLinkPointingTo(target: Path, vararg attributes: FileAttribute<*>): Path =
    Files.createSymbolicLink(this, target, *attributes)

/**
 * Reads the target of a symbolic link located by this path.
 *
 * @throws UnsupportedOperationException if symbolic links are not supported by this implementation.
 * @throws NotLinkException if the target is not a symbolic link
 *   (optional specific exception, some implementations may throw a more general one).
 *
 * @see Files.readSymbolicLink
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.readSymbolicLink(): Path =
    Files.readSymbolicLink(this)

/**
 * Creates a new and empty file specified by this path, failing if the file already exists.
 *
 * @param attributes an optional list of file attributes to set atomically when creating the file.
 *
 * @throws  FileAlreadyExistsException if a file specified by this path already exists
 *   (optional specific exception, some implementations may throw more general [IOException]).
 * @throws  UnsupportedOperationException if the [attributes] array contains an attribute that cannot be set atomically
 *   when creating the file.
 *
 * @see Files.createFile
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.createFile(vararg attributes: FileAttribute<*>): Path =
    Files.createFile(this, *attributes)

/**
 * Creates an empty file in the default temp directory, using
 * the given [prefix] and [suffix] to generate its name.
 *
 * @param attributes an optional list of file attributes to set atomically when creating the file.
 * @return the path to the newly created file that did not exist before.
 *
 * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically
 *   when creating the file.
 *
 * @see Files.createTempFile
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun createTempFile(prefix: String? = null, suffix: String? = null, vararg attributes: FileAttribute<*>): Path =
    Files.createTempFile(prefix, suffix, *attributes)

/**
 * Creates an empty file in the specified [directory], using
 * the given [prefix] and [suffix] to generate its name.
 *
 * @param directory the parent directory in which to create a new file.
 *   It can be `null`, in that case the new file is created in the default temp directory.
 * @param attributes an optional list of file attributes to set atomically when creating the file.
 * @return the path to the newly created file that did not exist before.
 *
 * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically
 *   when creating the file.
 *
 * @see Files.createTempFile
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
public fun createTempFile(directory: Path?, prefix: String? = null, suffix: String? = null, vararg attributes: FileAttribute<*>): Path =
    if (directory != null)
        Files.createTempFile(directory, prefix, suffix, *attributes)
    else
        Files.createTempFile(prefix, suffix, *attributes)

/**
 * Creates a new directory in the default temp directory, using the given [prefix] to generate its name.
 *
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 * @return the path to the newly created directory that did not exist before.
 *
 * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createTempDirectory
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun createTempDirectory(prefix: String? = null, vararg attributes: FileAttribute<*>): Path =
    Files.createTempDirectory(prefix, *attributes)

/**
 * Creates a new directory in the specified [directory], using the given [prefix] to generate its name.
 *
 * @param directory the parent directory in which to create a new directory.
 *   It can be `null`, in that case the new directory is created in the default temp directory.
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 * @return the path to the newly created directory that did not exist before.
 *
 * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createTempDirectory
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@Throws(IOException::class)
public fun createTempDirectory(directory: Path?, prefix: String? = null, vararg attributes: FileAttribute<*>): Path =
    if (directory != null)
        Files.createTempDirectory(directory, prefix, *attributes)
    else
        Files.createTempDirectory(prefix, *attributes)

/**
 * Resolves the given [other] path against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline operator fun Path.div(other: Path): Path =
    this.resolve(other)

/**
 * Resolves the given [other] path string against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline operator fun Path.div(other: String): Path =
    this.resolve(other)


/**
 * Converts the provided [path] string to a [Path] object of the [default][FileSystems.getDefault] filesystem.
 *
 * @see Paths.get
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path(path: String): Path =
    Paths.get(path)

/**
 * Converts the name sequence specified with the [base] path string and a number of [subpaths] additional names
 * to a [Path] object of the [default][FileSystems.getDefault] filesystem.
 *
 * @see Paths.get
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun Path(base: String, vararg subpaths: String): Path =
    Paths.get(base, *subpaths)

/**
 * Converts this URI to a [Path] object.
 *
 * @see Paths.get
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalPathApi::class)
@kotlin.internal.InlineOnly
public inline fun URI.toPath(): Path =
    Paths.get(this)


/**
 * Returns a sequence of paths for visiting this directory and all its content.
 *
 * By default, only files are visited, in depth-first order, and symbolic links are not followed.
 * The combination of [options] overrides the default behavior. See [PathWalkOption].
 *
 * The order in which sibling files are visited is unspecified.
 *
 * If after calling this function new files get added or deleted from the file tree rooted at this directory,
 * the changes may or may not appear in the returned sequence.
 *
 * If the file located by this path does not exist, an empty sequence is returned.
 * if the file located by this path is not a directory, a sequence containing only this path is returned.
 */
@ExperimentalPathApi
@SinceKotlin("1.7")
public fun Path.walk(vararg options: PathWalkOption): Sequence<Path> = PathTreeWalk(this, options)

/**
 * Visits this directory and all its content with the specified [visitor].
 *
 * The traversal is in depth-first order and starts at this directory. The specified [visitor] is invoked on each file encountered.
 *
 * @param visitor the [FileVisitor] that receives callbacks.
 * @param maxDepth the maximum depth to traverse. By default, there is no limit.
 * @param followLinks specifies whether to follow symbolic links, `false` by default.
 *
 * @see Files.walkFileTree
 */
@ExperimentalPathApi
@SinceKotlin("1.7")
public fun Path.visitFileTree(visitor: FileVisitor<Path>, maxDepth: Int = Int.MAX_VALUE, followLinks: Boolean = false): Unit {
    val options = if (followLinks) setOf(FileVisitOption.FOLLOW_LINKS) else setOf()
    Files.walkFileTree(this, options, maxDepth, visitor)
}

/**
 * Visits this directory and all its content with the [FileVisitor] defined in [builderAction].
 *
 * This function works the same as [Path.visitFileTree]. It is introduced to streamline
 * the cases when a [FileVisitor] is created only to be immediately used for a file tree traversal.
 * The trailing lambda [builderAction] is passed to [fileVisitor] to get the file visitor.
 *
 * Example:
 *
 * ``` kotlin
 * projectDirectory.visitFileTree {
 *     onPreVisitDirectory { directory, _ ->
 *         if (directory.name == "build") {
 *             directory.toFile().deleteRecursively()
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
 * }
 * ```
 *
 * @param maxDepth the maximum depth to traverse. By default, there is no limit.
 * @param followLinks specifies whether to follow symbolic links, `false` by default.
 * @param builderAction the function that defines [FileVisitor].
 *
 * @see Path.visitFileTree
 * @see fileVisitor
 */
@ExperimentalPathApi
@SinceKotlin("1.7")
public fun Path.visitFileTree(
    maxDepth: Int = Int.MAX_VALUE,
    followLinks: Boolean = false,
    builderAction: FileVisitorBuilder.() -> Unit
): Unit {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    visitFileTree(fileVisitor(builderAction), maxDepth, followLinks)
}

/**
 * Builds a [FileVisitor] whose implementation is defined in [builderAction].
 *
 * By default, the returned file visitor visits all files and re-throws I/O errors, that is:
 *   * [FileVisitor.preVisitDirectory] returns [FileVisitResult.CONTINUE].
 *   * [FileVisitor.visitFile] returns [FileVisitResult.CONTINUE].
 *   * [FileVisitor.visitFileFailed] re-throws the I/O exception that prevented the file from being visited.
 *   * [FileVisitor.postVisitDirectory] returns [FileVisitResult.CONTINUE] if the directory iteration completes without an I/O exception;
 *     otherwise it re-throws the I/O exception that caused the iteration of the directory to terminate prematurely.
 *
 * To override a function provide its implementation to the corresponding
 * function of the [FileVisitorBuilder] that was passed as a receiver to [builderAction].
 * Note that each function can be overridden only once.
 * Repeated override of a function throws [IllegalStateException].
 *
 * The builder is valid only inside [builderAction] function.
 * Using it outside the function throws [IllegalStateException].
 *
 * Example:
 *
 * ``` kotlin
 * val cleanVisitor = fileVisitor {
 *     onPreVisitDirectory { directory, _ ->
 *         if (directory.name == "build") {
 *             directory.toFile().deleteRecursively()
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
 * }
 * ```
 */
@ExperimentalPathApi
@SinceKotlin("1.7")
public fun fileVisitor(builderAction: FileVisitorBuilder.() -> Unit): FileVisitor<Path> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return FileVisitorBuilderImpl().apply(builderAction).build()
}
