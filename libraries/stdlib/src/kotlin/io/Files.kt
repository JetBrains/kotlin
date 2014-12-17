package kotlin.io

import java.io.*
import java.util.*

/**
 * Recursively process this file and all children with the given block.
 * Note that if this file doesn't exist, then the block will be executed on it anyway.
 */
public fun File.recurse(block: (File) -> Unit): Unit {
    block(this)
    listFiles()?.forEach { it.recurse(block) }
}

/**
 * Create an empty directory in the specified directory, using the given prefix and suffix to generate its name.
 * Prefix shouldn't be shorter than 3 symbols or IllegalArgumentException will be thrown.
 *
 * If prefix is not specified then some unspecified name will be used.
 * If suffix is not specified then ".tmp" will be used.
 * If directory is not specified then the default temporary-file directory will be used.
 *
 * Returns a file object corresponding to a newly-created directory.
 *
 * If some error occurs then IOException is thrown.
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
 * Create a new empty file in the specified directory, using the given prefix and suffix to generate its name.
 * Prefix shouldn't be shorter than 3 symbols or IllegalArgumentException will be thrown.
 *
 * If prefix is not specified then some unspecified name will be used.
 * If suffix is not specified then ".tmp" will be used.
 * If directory is not specified then the default temporary-file directory will be used.
 *
 * Returns a file object corresponding to a newly-created file.
 *
 * If some error occurs then IOException is thrown.
 */
public fun createTempFile(prefix: String = "tmp", suffix: String? = null, directory: File? = null): File {
    return File.createTempFile(prefix, suffix, directory)
}

/**
 * Returns this if the file is a directory or the parent if it is a file inside a directory
 */
public val File.directory: File
    get() = if (isDirectory()) this else getParentFile()!!

/**
 * Returns the canonical path of the file
 */
public val File.canonicalPath: String
    get() = getCanonicalPath()

/**
 * Returns the file name or "" for an empty name
 */
public val File.name: String
    get() = getName()

/**
 * Returns the file path or "" for an empty name
 */
public val File.path: String
    get() = getPath()

/**
 * Returns file's extension or an empty string if it doesn't have one
 */
public val File.extension: String
    get() {
        return name.substringAfterLast('.', "")
    }

/**
 * Replaces all separators in the string used to separate directories with system ones and returns the resulting string.
 */
public fun String.separatorsToSystem(): String {
    val otherSep = if (File.separator == "/") "\\" else "/"
    return replace(otherSep, File.separator)
}

/**
 * Replaces all path separators in the string with system ones and returns the resulting string.
 */
public fun String.pathSeparatorsToSystem(): String {
    val otherSep = if (File.pathSeparator == ":") ";" else ":"
    return replace(otherSep, File.pathSeparator)
}

/**
 * Replaces path and directories separators with corresponding system ones and returns the resulting string.
 */
public fun String.allSeparatorsToSystem(): String {
    return separatorsToSystem().pathSeparatorsToSystem()
}

/**
 * Returns a pathname of this file with all path separators replaced with File.pathSeparator
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
 * Returns true if the given file is in the same directory or a descendant directory
 */
public fun File.isDescendant(file: File): Boolean {
    return file.directory.canonicalPath.startsWith(directory.canonicalPath)
}

/**
 * Returns the relative path of the given descendant of this file if it is a descendant
 */
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
 * Copies this file to the given output file, returning the number of bytes copied
 */
public fun File.copyTo(file: File, bufferSize: Int = defaultBufferSize): Long {
    file.directory.mkdirs()
    val input = FileInputStream(this)
    return input.use<FileInputStream, Long>{
        val output = FileOutputStream(file)
        output.use<FileOutputStream, Long>{
            input.copyTo(output, bufferSize)
        }
    }
}

/**
 * Returns an array of files and directories in the directory that satisfy the specified filter
 * or null if this file does not denote a directory.
 */
public fun File.listFiles(filter: (file: File) -> Boolean): Array<File>? = listFiles(
        object : FileFilter {
            override fun accept(file: File) = filter(file)
        }
                                                                                    )
