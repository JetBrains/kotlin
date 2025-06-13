/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("FilesKt")

package kotlin.io

import java.io.File
import java.io.IOException

/**
 * Creates an empty directory in the specified [directory], using the given [prefix] and [suffix] to generate its name.
 *
 * If [prefix] is not specified then some unspecified string will be used.
 * If [suffix] is not specified then ".tmp" will be used.
 * If [directory] is not specified then the default temporary-file directory will be used.
 *
 * The [prefix] argument, if specified, must be at least three characters long.
 * It is recommended that the prefix be a short, meaningful string such as "job" or "mail".
 *
 * To create the new file, the [prefix] and the [suffix] may first be adjusted to fit the limitations of the underlying platform.
 *
 * **Note:** if the new directory is created in a directory that is shared with all users,
 * it may get permissions allowing everyone to read it or its content, thus creating a risk of leaking
 * sensitive information stored in this directory.
 * To avoid this, it's recommended either to specify an explicit parent [directory] that is not shared widely,
 * or to use alternative ways of creating temporary files,
 * such as [java.nio.file.Files.createTempDirectory](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#createTempDirectory-java.lang.String-java.nio.file.attribute.FileAttribute...-)
 * or the experimental `createTempDirectory` function in the `kotlin.io.path` package.
 *
 * @return a file object corresponding to a newly-created directory.
 *
 * @throws IOException in case of input/output error.
 * @throws IllegalArgumentException if [prefix] is shorter than three symbols.
 */
@Deprecated(
    "Avoid creating temporary directories in the default temp location with this function " +
    "due to too wide permissions on the newly created directory. " +
    "Use kotlin.io.path.createTempDirectory instead."
)
public fun createTempDir(prefix: String = "tmp", suffix: String? = null, directory: File? = null): File {
    val dir = File.createTempFile(prefix, suffix, directory)
    dir.delete()
    if (dir.mkdir()) {
        return dir
    } else {
        throw IOException("Unable to create temporary directory $dir.")
    }
}

/**
 * Creates a new empty file in the specified [directory], using the given [prefix] and [suffix] to generate its name.
 *
 * If [prefix] is not specified then some unspecified string will be used.
 * If [suffix] is not specified then ".tmp" will be used.
 * If [directory] is not specified then the default temporary-file directory will be used.
 *
 * The [prefix] argument, if specified, must be at least three characters long.
 * It is recommended that the prefix be a short, meaningful string such as "job" or "mail".
 *
 * To create the new file, the [prefix] and the [suffix] may first be adjusted to fit the limitations of the underlying platform.
 *
 * **Note:** if the new file is created in a directory that is shared with all users,
 * it may get permissions allowing everyone to read it, thus creating a risk of leaking
 * sensitive information stored in this file.
 * To avoid this, it's recommended either to specify an explicit parent [directory] that is not shared widely,
 * or to use alternative ways of creating temporary files,
 * such as [java.nio.file.Files.createTempFile](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#createTempFile-java.lang.String-java.lang.String-java.nio.file.attribute.FileAttribute...-)
 * or the experimental `createTempFile` function in the `kotlin.io.path` package.
 *
 * @return a file object corresponding to a newly-created file.
 *
 * @throws IOException in case of input/output error.
 * @throws IllegalArgumentException if [prefix] is shorter than three symbols.
 */
@Deprecated(
    "Avoid creating temporary files in the default temp location with this function " +
    "due to too wide permissions on the newly created file. " +
    "Use kotlin.io.path.createTempFile instead or resort to java.io.File.createTempFile."
)
public fun createTempFile(prefix: String = "tmp", suffix: String? = null, directory: File? = null): File {
    return File.createTempFile(prefix, suffix, directory)
}

/**
 * Returns the extension of this file (not including the dot), or an empty string if it doesn't have one.
 */
public val File.extension: String
    get() = name.substringAfterLast('.', "")

/**
 * Returns [path][File.path] of this File using the invariant separator '/' to
 * separate the names in the name sequence.
 */
public val File.invariantSeparatorsPath: String
    get() = if (File.separatorChar != '/') path.replace(File.separatorChar, '/') else path

/**
 * Returns file's name without an extension.
 */
public val File.nameWithoutExtension: String
    get() = name.substringBeforeLast(".")

/**
 * Calculates the relative path for this file from [base] file.
 * Note that the [base] file is treated as a directory.
 * If this file matches the [base] file, then an empty string will be returned.
 *
 * @return relative path from [base] to this.
 *
 * @throws IllegalArgumentException if this and base paths have different roots.
 */
public fun File.toRelativeString(base: File): String =
    toRelativeStringOrNull(base) ?: throw IllegalArgumentException("this and base files have different roots: $this and $base.")

/**
 * Calculates the relative path for this file from [base] file.
 * Note that the [base] file is treated as a directory.
 * If this file matches the [base] file, then a [File] with empty path will be returned.
 *
 * @return File with relative path from [base] to this.
 *
 * @throws IllegalArgumentException if this and base paths have different roots.
 */
public fun File.relativeTo(base: File): File = File(this.toRelativeString(base))

/**
 * Calculates the relative path for this file from [base] file.
 * Note that the [base] file is treated as a directory.
 * If this file matches the [base] file, then a [File] with empty path will be returned.
 *
 * @return File with relative path from [base] to this, or `this` if this and base paths have different roots.
 */
public fun File.relativeToOrSelf(base: File): File =
    toRelativeStringOrNull(base)?.let(::File) ?: this

/**
 * Calculates the relative path for this file from [base] file.
 * Note that the [base] file is treated as a directory.
 * If this file matches the [base] file, then a [File] with empty path will be returned.
 *
 * @return File with relative path from [base] to this, or `null` if this and base paths have different roots.
 */
public fun File.relativeToOrNull(base: File): File? =
    toRelativeStringOrNull(base)?.let(::File)


private fun File.toRelativeStringOrNull(base: File): String? {
    // Check roots
    val thisComponents = this.toComponents().normalize()
    val baseComponents = base.toComponents().normalize()
    if (thisComponents.root != baseComponents.root) {
        return null
    }

    val baseCount = baseComponents.size
    val thisCount = thisComponents.size

    val sameCount = run countSame@{
        var i = 0
        val maxSameCount = minOf(thisCount, baseCount)
        while (i < maxSameCount && thisComponents.segments[i] == baseComponents.segments[i])
            i++
        return@countSame i
    }

    // Annihilate differing base components by adding required number of .. parts
    val res = StringBuilder()
    for (i in baseCount - 1 downTo sameCount) {
        if (baseComponents.segments[i].name == "..") {
            return null
        }

        res.append("..")

        if (i != sameCount) {
            res.append(File.separatorChar)
        }
    }

    // Add remaining this components
    if (sameCount < thisCount) {
        // If some .. were appended
        if (sameCount < baseCount)
            res.append(File.separatorChar)

        thisComponents.segments.drop(sameCount).joinTo(res, File.separator)
    }

    return res.toString()
}


/**
 * Copies this file to the given [target] file.
 *
 * If some directories on a way to the [target] are missing, then they will be created.
 * If the [target] file already exists, this function will fail unless [overwrite] argument is set to `true`.
 *
 * When [overwrite] is `true` and [target] is a directory, it is replaced only if it is empty.
 *
 * If this file is a directory, it is copied without its content, i.e. an empty [target] directory is created.
 * If you want to copy directory including its contents, use [copyRecursively].
 *
 * The operation doesn't preserve copied file attributes such as creation/modification date, permissions, etc.
 *
 * @param overwrite `true` if destination overwrite is allowed.
 * @param bufferSize the buffer size to use when copying.
 * @return the [target] file.
 * @throws NoSuchFileException if the source file doesn't exist.
 * @throws FileAlreadyExistsException if the destination file already exists and [overwrite] argument is set to `false`.
 * @throws IOException if any errors occur while copying.
 */
public fun File.copyTo(target: File, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): File {
    if (!this.exists()) {
        throw NoSuchFileException(file = this, reason = "The source file doesn't exist.")
    }

    if (target.exists()) {
        if (!overwrite)
            throw FileAlreadyExistsException(file = this, other = target, reason = "The destination file already exists.")
        else if (!target.delete())
            throw FileAlreadyExistsException(file = this, other = target, reason = "Tried to overwrite the destination, but failed to delete it.")
    }

    if (this.isDirectory) {
        if (!target.mkdirs())
            throw FileSystemException(file = this, other = target, reason = "Failed to create target directory.")
    } else {
        target.parentFile?.mkdirs()

        this.inputStream().use { input ->
            target.outputStream().use { output ->
                val _ = input.copyTo(output, bufferSize)
            }
        }
    }

    return target
}

/**
 * Enum that can be used to specify behaviour of the `copyRecursively()` function
 * in exceptional conditions.
 */
public enum class OnErrorAction {
    /** Skip this file and go to the next. */
    SKIP,

    /** Terminate the evaluation of the function. */
    TERMINATE
}

/** Private exception class, used to terminate recursive copying. */
private class TerminateException(file: File) : FileSystemException(file) {}

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
public fun File.copyRecursively(
    target: File,
    overwrite: Boolean = false,
    onError: (File, IOException) -> OnErrorAction = { _, exception -> throw exception }
): Boolean {
    if (!exists()) {
        return onError(this, NoSuchFileException(file = this, reason = "The source file doesn't exist.")) !=
                OnErrorAction.TERMINATE
    }
    try {
        // We cannot break for loop from inside a lambda, so we have to use an exception here
        for (src in walkTopDown().onFail { f, e -> if (onError(f, e) == OnErrorAction.TERMINATE) throw TerminateException(f) }) {
            if (!src.exists()) {
                if (onError(src, NoSuchFileException(file = src, reason = "The source file doesn't exist.")) ==
                        OnErrorAction.TERMINATE)
                    return false
            } else {
                val relPath = src.toRelativeString(this)
                val dstFile = File(target, relPath)
                if (dstFile.exists() && !(src.isDirectory && dstFile.isDirectory)) {
                    val stillExists = if (!overwrite) true else {
                        if (dstFile.isDirectory)
                            !dstFile.deleteRecursively()
                        else
                            !dstFile.delete()
                    }

                    if (stillExists) {
                        if (onError(dstFile, FileAlreadyExistsException(file = src,
                                    other = dstFile,
                                    reason = "The destination file already exists.")) == OnErrorAction.TERMINATE)
                                return false

                        continue
                    }
                }

                if (src.isDirectory) {
                    dstFile.mkdirs()
                } else {
                    if (src.copyTo(dstFile, overwrite).length() != src.length()) {
                        if (onError(src, IOException("Source file wasn't copied completely, length of destination file differs.")) == OnErrorAction.TERMINATE)
                            return false
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
public fun File.deleteRecursively(): Boolean = walkBottomUp().fold(true, { res, it -> (it.delete() || !it.exists()) && res })

/**
 * Determines whether this file belongs to the same root as [other]
 * and starts with all components of [other] in the same order.
 * So if [other] has N components, first N components of `this` must be the same as in [other].
 *
 * @return `true` if this path starts with [other] path, `false` otherwise.
 */
public fun File.startsWith(other: File): Boolean {
    val components = toComponents()
    val otherComponents = other.toComponents()
    if (components.root != otherComponents.root)
        return false
    return if (components.size < otherComponents.size) false
    else components.segments.subList(0, otherComponents.size).equals(otherComponents.segments)
}

/**
 * Determines whether this file belongs to the same root as [other]
 * and starts with all components of [other] in the same order.
 * So if [other] has N components, first N components of `this` must be the same as in [other].
 *
 * @return `true` if this path starts with [other] path, `false` otherwise.
 */
public fun File.startsWith(other: String): Boolean = startsWith(File(other))

/**
 * Determines whether this file path ends with the path of [other] file.
 *
 * If [other] is rooted path it must be equal to this.
 * If [other] is relative path then last N components of `this` must be the same as all components in [other],
 * where N is the number of components in [other].
 *
 * @return `true` if this path ends with [other] path, `false` otherwise.
 */
public fun File.endsWith(other: File): Boolean {
    val components = toComponents()
    val otherComponents = other.toComponents()
    if (otherComponents.isRooted)
        return this == other
    val shift = components.size - otherComponents.size
    return if (shift < 0) false
    else components.segments.subList(shift, components.size).equals(otherComponents.segments)
}

/**
 * Determines whether this file belongs to the same root as [other]
 * and ends with all components of [other] in the same order.
 * So if [other] has N components, last N components of `this` must be the same as in [other].
 * For relative [other], `this` can belong to any root.
 *
 * @return `true` if this path ends with [other] path, `false` otherwise.
 */
public fun File.endsWith(other: String): Boolean = endsWith(File(other))

/**
 * Removes all . and resolves all possible .. in this file name.
 * For instance, `File("/foo/./bar/gav/../baaz").normalize()` is `File("/foo/bar/baaz")`.
 *
 * @return normalized pathname with . and possibly .. removed.
 */
public fun File.normalize(): File =
    with(toComponents()) { root.resolve(segments.normalize().joinToString(File.separator)) }

private fun FilePathComponents.normalize(): FilePathComponents =
    FilePathComponents(root, segments.normalize())

private fun List<File>.normalize(): List<File> {
    val list: MutableList<File> = ArrayList(this.size)
    for (file in this) {
        when (file.name) {
            "." -> {}
            ".." -> if (!list.isEmpty() && list.last().name != "..") list.removeAt(list.size - 1) else list.add(file)
            else -> list.add(file)
        }
    }
    return list
}

/**
 * Adds [relative] file to this, considering this as a directory.
 * If [relative] has a root, [relative] is returned back.
 * For instance, `File("/foo/bar").resolve(File("gav"))` is `File("/foo/bar/gav")`.
 * This function is complementary with [relativeTo],
 * so `f.resolve(g.relativeTo(f)) == g` should be always `true` except for different roots case.
 *
 * @return concatenated this and [relative] paths, or just [relative] if it's absolute.
 */
public fun File.resolve(relative: File): File {
    if (relative.isRooted)
        return relative
    val baseName = this.toString()
    return if (baseName.isEmpty() || baseName.endsWith(File.separatorChar)) File(baseName + relative) else File(baseName + File.separatorChar + relative)
}

/**
 * Adds [relative] name to this, considering this as a directory.
 * If [relative] has a root, [relative] is returned back.
 * For instance, `File("/foo/bar").resolve("gav")` is `File("/foo/bar/gav")`.
 *
 * @return concatenated this and [relative] paths, or just [relative] if it's absolute.
 */
public fun File.resolve(relative: String): File = resolve(File(relative))

/**
 * Adds [relative] file to this parent directory.
 * If [relative] has a root or this has no parent directory, [relative] is returned back.
 * For instance, `File("/foo/bar").resolveSibling(File("gav"))` is `File("/foo/gav")`.
 *
 * @return concatenated this.parent and [relative] paths, or just [relative] if it's absolute or this has no parent.
 */
public fun File.resolveSibling(relative: File): File {
    val components = this.toComponents()
    val parentSubPath = if (components.size == 0) File("..") else components.subPath(0, components.size - 1)
    return components.root.resolve(parentSubPath).resolve(relative)
}

/**
 * Adds [relative] name to this parent directory.
 * If [relative] has a root or this has no parent directory, [relative] is returned back.
 * For instance, `File("/foo/bar").resolveSibling("gav")` is `File("/foo/gav")`.
 *
 * @return concatenated this.parent and [relative] paths, or just [relative] if it's absolute or this has no parent.
 */
public fun File.resolveSibling(relative: String): File = resolveSibling(File(relative))
