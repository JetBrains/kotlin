package kotlin.io

import java.io.*
import java.nio.charset.*
import java.util.*

/**
 * Recursively process this file and all children with the given block
 */
public fun File.recurse(block: (File) -> Unit): Unit {
    block(this)
    val children = listFiles()
    if (children != null) {
        for (child in children) {
            child.recurse(block)
        }
    }
}

/**
 * Returns this if the file is a directory or the parent if its a file inside a directory
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
 * Returns true if the file ends with the given extension
 */
public val File.extension: String
    get() {
        val text = name
        val idx = text.lastIndexOf('.')
        return if (idx >= 0) {
            text.substring(idx + 1)
        } else {
            ""
        }
    }

/**
 * Returns true if the given file is in the same directory or a descendant directory
 */
public fun File.isDescendant(file: File): Boolean {
    return file.directory.canonicalPath.startsWith(directory.canonicalPath)
}

/**
 * Returns the relative path of the given descendant of this file if its a descendant
 */
public fun File.relativePath(descendant: File): String {
    val prefix = directory.canonicalPath
    val answer = descendant.canonicalPath
    return if (answer.startsWith(prefix)) {
        val prefixSize = prefix.size
        if (answer.size > prefixSize) {
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
 * Returns an array of files and directories in the directory that satisfy the specified filter.
 */
public fun File.listFiles(filter: (file: File) -> Boolean): Array<File>? = listFiles(
        object : FileFilter {
            override fun accept(file: File) = filter(file)
        }
                                                                                    )
