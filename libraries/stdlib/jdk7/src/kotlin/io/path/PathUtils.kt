/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

/**
 * Returns the name of the file or directory denoted by this path as a string,
 * or an empty string if this path has zero path elements.
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
public val Path.name: String
    get() = fileName?.toString().orEmpty()

/**
 * Returns the [name][Path.name] of this file or directory without an extension,
 * or an empty string if this path has zero path elements.
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
public val Path.nameWithoutExtension: String
    get() = fileName?.toString()?.substringBeforeLast(".") ?: ""

/**
 * Returns the extension of this path (not including the dot),
 * or an empty string if it doesn't have one.
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
public val Path.extension: String
    get() = fileName?.toString()?.substringAfterLast('.', "") ?: ""

/**
 * Returns this path as a [String] using the invariant separator '/' to
 * separate the names in the name sequence.
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
public val Path.invariantSeparatorsPath: String
    get() {
        val separator = fileSystem.separator
        return if (separator != "/") toString().replace(separator, "/") else toString()
    }


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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
 *
 * @see Files.copy
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.copyTo(target: Path, overwrite: Boolean = false): Path {
    val options = if (overwrite) arrayOf<CopyOption>(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
    return Files.copy(this, target, *options)
}

/**
 * Copies a file or directory located by this path to the given [target] path.
 *
 * Unlike `File.copyTo`, if some directories on the way to the [target] are missing, then they won't be created automatically.
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
 *
 * @see Files.copy
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.notExists(vararg options: LinkOption): Boolean = Files.notExists(this, *options)

/**
 * Checks if the file located by this path is a regular file.
 *
 * @param options options to control how symbolic links are handled.
 *
 * @see Files.isRegularFile
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.isDirectory(vararg options: LinkOption): Boolean = Files.isDirectory(this, *options)

/**
 * Checks if the file located by this path exists and is a symbolic link.
 *
 * @see Files.isSymbolicLink
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.isSymbolicLink(): Boolean = Files.isSymbolicLink(this)

/**
 * Checks if the file located by this path exists and is executable.
 *
 * @see Files.isExecutable
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.isHidden(): Boolean = Files.isHidden(this)

/**
 * Checks if the file located by this path exists and is readable.
 *
 * @see Files.isReadable
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.isReadable(): Boolean = Files.isReadable(this)

/**
 * Checks if the file located by this path exists and is writable.
 *
 * @see Files.isWritable
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.isWritable(): Boolean = Files.isWritable(this)

/**
 * Checks if the file located by this path points to the same file or directory as [other].
 *
 * @see Files.isSameFile
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.createDirectory(vararg attributes: FileAttribute<*>): Path =
    Files.createDirectory(this, *attributes)

/**
 * Creates a directory ensuring that all nonexistent parent directories exist by creating them first.
 *
 * If the directory already exists, this function does not throw an exception, unlike [Path.createDirectory].
 *
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 *
 * @throws FileAlreadyExistsException if there is already a file located by this path
 * (optional specific exception, some implementations may throw more general [IOException]).
 * @throws IOException if an I/O error occurs.
 * @throws UnsupportedOperationException if the [attributes ]array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createDirectories
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path.createDirectories(vararg attributes: FileAttribute<*>): Path =
    Files.createDirectories(this, *attributes)


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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
@SinceKotlin("1.4")
@ExperimentalPathApi
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
 * @throws  UnsupportedOperationException if the array contains an attribute that cannot be set atomically
 *   when creating the file.
 *
 * @see Files.createTempFile
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun createTempFile(prefix: String? = null, suffix: String? = null, vararg attributes: FileAttribute<*>): Path =
    Files.createTempFile(prefix, suffix, *attributes)

/**
 * Creates an empty file in the specified [directory], using
 * the given [prefix] and [suffix] to generate its name.
 *
 * @param attributes an optional list of file attributes to set atomically when creating the file.
 * @return the path to the newly created file that did not exist before.
 *
 * @throws  UnsupportedOperationException if the array contains an attribute that cannot be set atomically
 *   when creating the file.
 *
 * @see Files.createTempFile
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun createTempFile(directory: Path, prefix: String? = null, suffix: String? = null, vararg attributes: FileAttribute<*>): Path =
    Files.createTempFile(directory, prefix, suffix, *attributes)

/**
 * Creates a new directory in the default temp directory, using the given [prefix] to generate its name.
 *
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 * @return the path to the newly created directory that did not exist before.
 *
 * @throws  UnsupportedOperationException if the array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createTempDirectory
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun createTempDirectory(prefix: String? = null, vararg attributes: FileAttribute<*>): Path =
    Files.createTempDirectory(prefix, *attributes)

/**
 * Creates a new directory in the specified [directory], using the given [prefix] to generate its name.
 *
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 * @return the path to the newly created directory that did not exist before.
 *
 * @throws  UnsupportedOperationException if the array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createTempDirectory
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun createTempDirectory(directory: Path, prefix: String? = null, vararg attributes: FileAttribute<*>): Path =
    Files.createTempDirectory(directory, prefix, *attributes)

/**
 * Resolves the given [other] path against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline operator fun Path.div(other: Path): Path =
    this.resolve(other)

/**
 * Resolves the given [other] path string against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline operator fun Path.div(other: String): Path =
    this.resolve(other)


/**
 * Converts the provided [path] string to a [Path] object of the [default][FileSystems.getDefault] filesystem.
 *
 * @see Paths.get
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path(path: String): Path =
    Paths.get(path)

/**
 * Converts the name sequence specified with the [base] path string and a number of [subpaths] additional names
 * to a [Path] object of the [default][FileSystems.getDefault] filesystem.
 *
 * @see Paths.get
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun Path(base: String, vararg subpaths: String): Path =
    Paths.get(base, *subpaths)

/**
 * Converts this URI to a [Path] object.
 *
 * @see Paths.get
 */
@SinceKotlin("1.4")
@ExperimentalPathApi
@kotlin.internal.InlineOnly
public inline fun URI.toPath(): Path =
    Paths.get(this)
