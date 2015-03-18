package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.util.ArrayList

/**
 * Creates an empty directory in the specified [directory], using the given [prefix] and [suffix] to generate its name.
 *
 * If [prefix] is not specified then some unspecified name will be used.
 * If [suffix] is not specified then ".tmp" will be used.
 * If [directory] is not specified then the default temporary-file directory will be used.
 *
 * @return a file object corresponding to a newly-created directory.
*
 * @throws IOException in case of input/output error
 * @throws IllegalArgumentException if [prefix] is shorter than three symbols
 */
public fun createTempDir(prefix: String = "tmp", suffix: String? = null, directory: File? = null): File {
    val dir = File.createTempFile(prefix, suffix, directory)
    dir.delete()
    if (dir.mkdir()) {
        return dir
    } else {
        throw IOException("Unable to create temporary directory")
    }
}

/**
 * Creates a new empty file in the specified [directory], using the given [prefix] and [suffix] to generate its name.
 *
 * If [prefix] is not specified then some unspecified name will be used.
 * If [suffix] is not specified then ".tmp" will be used.
 * If [directory] is not specified then the default temporary-file directory will be used.
 *
 * @return a file object corresponding to a newly-created file.
*
 * @throws IOException in case of input/output error
 * @throws IllegalArgumentException if [prefix] is shorter than three symbols
 */
public fun createTempFile(prefix: String = "tmp", suffix: String? = null, directory: File? = null): File {
    return File.createTempFile(prefix, suffix, directory)
}

/**
 * Returns this if this file is a directory, or the parent if it is a file inside a directory
 */
public val File.directory: File
    get() = if (isDirectory()) this else parent!!

/**
 * Returns parent of this abstract path name, or null if it has no parent
 */
public val File.parent: File?
    get() = getParentFile()

/**
 * Returns the canonical path of this file.
 */
public val File.canonicalPath: String
    get() = getCanonicalPath()

/**
 * Returns the file name
 */
public val File.name: String
    get() = getName()

/**
 * Returns the file path
 */
public val File.path: String
    get() = getPath()

/**
 * Returns the extension of this file (not including the dot), or an empty string if it doesn't have one.
 */
public val File.extension: String
    get() {
        return name.substringAfterLast('.', "")
    }

/**
 * Replaces all separators in the string used to separate directories with system ones and returns the resulting string.
 *
 * @return the pathname with system separators
 */
public fun String.separatorsToSystem(): String {
    val otherSep = if (File.separator == "/") "\\" else "/"
    return replace(otherSep, File.separator)
}

/**
 * Replaces all path separators in the string with system ones and returns the resulting string.
 *
 * @return the pathname with system separators
 */
public fun String.pathSeparatorsToSystem(): String {
    val otherSep = if (File.pathSeparator == ":") ";" else ":"
    return replace(otherSep, File.pathSeparator)
}

/**
 * Replaces path and directories separators with corresponding system ones and returns the resulting string.
 *
 * @return the pathname with system separators
 */
public fun String.allSeparatorsToSystem(): String {
    return separatorsToSystem().pathSeparatorsToSystem()
}

/** Creates a new reader for the string */
public fun String.reader(): StringReader = StringReader(this)

/** Creates a new byte input stream for the string */
public fun String.byteInputStream(charset: Charset = Charsets.UTF_8): InputStream = ByteArrayInputStream(toByteArray(charset))

/**
 * Returns a pathname of this file with all path separators replaced with File.pathSeparator
 *
 * @return the pathname with system separators
 */
public fun File.separatorsToSystem(): String {
    return toString().separatorsToSystem()
}

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
 * @return relative path from [base] to this
*
 * @throws IllegalArgumentException if child and parent have different roots.
 */
public fun File.relativeTo(base: File): String {
    // Check roots
    val components = filePathComponents()
    val baseComponents = base.filePathComponents()
    if (components.rootName != base.rootName)
        throw IllegalArgumentException("this and base files have different roots")
    var i = 0
    while (i < components.size && i < baseComponents.size && components.fileList[i] == baseComponents.fileList[i])
        i++
    val sameCount = i
    val baseCount = baseComponents.size
    // Add all ..
    var res = ""
    for (j in sameCount + 1..baseCount - 1)
        res += (".." + File.separator)
    // If .. is the last element, no separator should present
    if (baseCount > sameCount) {
        res += if (sameCount < components.size) ".." + File.separator else ".."
    }
    // Add remaining this components
    if (sameCount < components.size - 1)
        res += (components.subPath(sameCount, components.size - 1).toString() + File.separator)
    // The last one should be without separator
    if (sameCount < components.size)
        res += components.subPath(components.size - 1, components.size).toString()
    return res
}

/**
 * Calculates the relative path for this file from [descendant] file.
 * Note that the [descendant] file is treated as a directory.
 * If this file matches the [descendant] directory or does not belong to it,
 * then an empty string will be returned.
 */
deprecated("Use relativeTo() function instead")
public fun File.relativePath(descendant: File): String {
    val prefix = directory.canonicalPath
    val answer = descendant.canonicalPath
    return if (answer.startsWith(prefix)) {
        val prefixSize = prefix.length()
        if (answer.length() > prefixSize) {
            answer.substring(prefixSize + 1)
        } else ""
    } else {
        answer
    }
}

/**
 * Copies this file to the given output [dst], returning the number of bytes copied.
 *
 * If some directories on a way to the [dst] are missing, then they will be created.
 * If the [dst] file already exists, then this function will fail unless [overwrite] argument is set to true and
 * the [dst] file is not a non-empty directory.
 *
 * Note: this function fails if you call it on a directory.
 * If you want to copy directories, use 'copyRecursively' function instead.
 *
 * @param overwrite true if destination overwrite is allowed
 * @param bufferSize the buffer size to use when copying.
 * @return the number of bytes copied
 * @throws NoSuchFileException if the source file doesn't exist
 * @throws FileAlreadyExistsException if the destination file already exists and 'rewrite' argument is set to false
 * @throws IOException if any errors occur while copying
 */
public fun File.copyTo(dst: File, overwrite: Boolean = false, bufferSize: Int = defaultBufferSize): Long {
    if (!exists()) {
        throw NoSuchFileException(file = this, reason = "The source file doesn't exist")
    } else if (isDirectory()) {
        throw IllegalArgumentException("Use copyRecursively to copy a directory")
    } else if (dst.exists()) {
        if (!overwrite) {
            throw FileAlreadyExistsException(file = this,
                    other = dst,
                    reason = "The destination file already exists")
        } else if (dst.isDirectory() && dst.listFiles().any()) {
            // In this case file should be copied *into* this directory,
            // no matter whether it is empty or not
            return copyTo(dst.resolve(name), overwrite, bufferSize)
        }
    }
    dst.getParentFile().mkdirs()
    dst.delete()
    val input = FileInputStream(this)
    return input.use<FileInputStream, Long> {
        val output = FileOutputStream(dst)
        output.use<FileOutputStream, Long> {
            input.copyTo(output, bufferSize)
        }
    }
}

/**
 * Enum that can be used to specify behaviour of the `copyRecursively()` function
 * in exceptional conditions.
 */
public enum class OnErrorAction {
    /** Skip this file and go to the next. */
    SKIP

    /** Terminate the evaluation of the function. */
    TERMINATE
}

private class TerminateException(file: File) : FileSystemException(file) {}

/**
 * Copies this file with all its children to the specified destination [dst] path.
 * If some directories on the way to the destination are missing, then they will be created.
 *
 * If any errors occur during the copying, then further actions will depend on the result of the call
 * to `onError(File, IOException)` function, that will be called with arguments,
 * specifying the file that caused the error and the exception itself.
 * By default this function rethrows exceptions.
 * Exceptions that can be passed to the `onError` function:
 * NoSuchFileException - if there was an attempt to copy a non-existent file
 * FileAlreadyExistsException - if there is a conflict
 * AccessDeniedException - if there was an attempt to open a directory that didn't succeed.
 * IOException - if some problems occur when copying.
 *
 * @return false if the copying was terminated, true otherwise.
*
* Note that if this function fails, then partial copying may have taken place.
 */
public fun File.copyRecursively(dst: File,
                                onError: (File, IOException) -> OnErrorAction =
                                { file, e -> throw e }
): Boolean {
    if (!exists()) {
        return onError(this, NoSuchFileException(file = this, reason = "The source file doesn't exist")) !=
                OnErrorAction.TERMINATE
    }
    try {
        for (src in walkTopDown().fail { f, e -> if (onError(f, e) == OnErrorAction.TERMINATE) throw TerminateException(f) }) {
            if (!src.exists()) {
                if (onError(src, NoSuchFileException(file = src, reason = "The source file doesn't exist")) ==
                        OnErrorAction.TERMINATE)
                    return false
            } else {
                val relPath = src.relativeTo(this)
                val dstFile = File(dst, relPath)
                if (dstFile.exists() && !(src.isDirectory() && dstFile.isDirectory())) {
                    if (onError(dstFile, FileAlreadyExistsException(file = src,
                            other = dstFile,
                            reason = "The destination file already exists")) == OnErrorAction.TERMINATE)
                        return false
                } else if (src.isDirectory()) {
                    dstFile.mkdirs()
                } else {
                    if (src.copyTo(dstFile, true) != src.length()) {
                        if (onError(src, IOException("src.length() != dst.length()")) == OnErrorAction.TERMINATE)
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
 * @return true if the file or directory is successfully deleted, false otherwise.
 */
public fun File.deleteRecursively(): Boolean {
    var result = exists()
    walkBottomUp().forEach { if (!it.delete()) result = false }
    return result
}

/**
 * Returns an array of files and directories in the directory that match the specified [filter]
 * or null if this file does not denote a directory.
 */
public fun File.listFiles(filter: (file: File) -> Boolean): Array<File>? = listFiles(
        object : FileFilter {
            override fun accept(file: File) = filter(file)
        }
)

/**
 * Determines whether this file belongs to the same root as [o]
 * and starts with all components of [o] in the same order.
 * So if [o] has N components, first N components of `this` must be the same as in [o].
 * For relative [o], `this` can belong to any root.
 *
 * @return true if this path starts with [o] path, false otherwise
 */
public fun File.startsWith(o: File): Boolean {
    val components = filePathComponents()
    val otherComponents = o.filePathComponents()
    if (components.rootName != otherComponents.rootName && otherComponents.rootName != "")
        return false
    if (components.size < otherComponents.size)
        return false
    for (i in 0..otherComponents.size - 1) {
        if (components.fileList[i] != otherComponents.fileList[i])
            return false
    }
    return true
}

/**
 * Determines whether this file belongs to the same root as [o]
 * and starts with all components of [o] in the same order.
 * So if [o] has N components, first N components of `this` must be the same as in [o].
 * For relative [o], `this` can belong to any root.
 *
 * @return true if this path starts with [o] path, false otherwise
 */
public fun File.startsWith(o: String): Boolean = startsWith(File(o))

/**
 * Determines whether this file belongs to the same root as [o]
 * and ends with all components of [o] in the same order.
 * So if [o] has N components, last N components of `this` must be the same as in [o].
 * For relative [o], `this` can belong to any root.
 *
 * @return true if this path ends with [o] path, false otherwise
 */
public fun File.endsWith(o: File): Boolean {
    val components = filePathComponents()
    val otherComponents = o.filePathComponents()
    if (components.rootName != otherComponents.rootName && otherComponents.rootName != "")
        return false
    val shift = components.size - otherComponents.size
    if (shift < 0)
        return false
    for (i in 0..otherComponents.size - 1) {
        if (components.fileList[i + shift] != otherComponents.fileList[i])
            return false
    }
    return true
}

/**
 * Determines whether this file belongs to the same root as [o]
 * and ends with all components of [o] in the same order.
 * So if [o] has N components, last N components of `this` must be the same as in [o].
 * For relative [o], `this` can belong to any root.
 *
 * @return true if this path ends with [o] path, false otherwise
 */
public fun File.endsWith(o: String): Boolean = endsWith(File(o))

/**
 * Removes all . and resolves all possible .. in this file name.
 * For instance, File("/foo/./bar/gav/../baaz").normalize is File("/foo/bar/baaz")
 *
 * @return normalized pathname with . and possibly .. removed
 */
public fun File.normalize(): File {
    val components = filePathComponents()
    val rootName = components.rootName
    val list: MutableList<String> = components.fileList.filter { it.toString() != "." }.map { it.toString() }.toLinkedList()
    var first = 0
    var dots = list.subList(first, list.size()).indexOf("..")
    while (dots != -1) {
        if (dots > 0) {
            list.remove(dots + first)
            list.remove(dots + first - 1)
        } else {
            first++
        }
        dots = list.subList(first, list.size()).indexOf("..")
    }
    var res = rootName
    var addSeparator = rootName != "" && !rootName.endsWith(File.separatorChar)
    for (elem in list) {
        if (addSeparator)
            res += File.separatorChar
        res += elem
        addSeparator = true
    }
    return File(res)
}

/**
 * Adds relative [o] to this, considering this as a directory.
 * If [o] has a root, [o] is returned back.
 * For instance, File("/foo/bar").resolve(File("gav")) is File("/foo/bar/gav").
 * This function is complementary with (File.relativeTo),
 * so f.resolve(g.relativeTo(f)) == g should be always true except for different roots case.
 *
 * @return concatenated this and [o] paths, or just [o] if it's absolute
 */
public fun File.resolve(o: File): File {
    if (o.root != null)
        return o
    val ourName = toString()
    return if (ourName.endsWith(File.separatorChar)) File(ourName + o) else File(ourName + File.separatorChar + o)
}

/**
 * Adds relative [o] to this, considering this as a directory.
 * If [o] has a root, [o] is returned back.
 * For instance, File("/foo/bar").resolve("gav") is File("/foo/bar/gav")
 *
 * @return concatenated this and [o] paths, or just [o] if it's absolute
 */
public fun File.resolve(o: String): File = resolve(File(o))

/**
 * Adds relative [o] to this parent directory.
 * If [o] has a root or this has no parent directory, [o] is returned back.
 * For instance, File("/foo/bar").resolveSibling(File("gav")) is File("/foo/gav")
 *
 * @return concatenated this.parent and [o] paths, or just [o] if it's absolute or this has no parent
 */
public fun File.resolveSibling(o: File): File {
    val components = filePathComponents()
    val rootName = components.rootName
    val parentFile = when (components.size) {
        0 -> null
        1 -> File(rootName)
        else -> File(rootName + components.subPath(0, components.size - 1).path)
    }
    return if (parentFile != null) parentFile.resolve(o) else o
}

/**
 * Adds relative [o] to this parent directory.
 * If [o] has a root or this has no parent directory, [o] is returned back
 * For instance, File("/foo/bar").resolveSibling("gav") is File("/foo/gav")
 *
 * @return concatenated this.parent and [o] paths, or just [o] if it's absolute or this has no parent
 */
public fun File.resolveSibling(o: String): File = resolveSibling(File(o))
