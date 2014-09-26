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
 * Creates a new [[FileReader]] for this file
 */
public fun File.reader(): FileReader = FileReader(this)

/**
 * Reads the entire content of the file as bytes
 *
 * This method is not recommended on huge files.
 */
public fun File.readBytes(): ByteArray {
    return FileInputStream(this).use { it.readBytes(length().toInt()) }
}

/**
 * Writes the bytes as the contents of the file
 */
public fun File.writeBytes(data: ByteArray): Unit {
    return FileOutputStream(this).use { it.write(data) }
}

/**
 * Appends bytes to the contents of the file.
 */
public fun File.appendBytes(data: ByteArray): Unit {
    return FileOutputStream(this, true).use { it.write(data) }
}

/**
 * Reads the entire content of the file as a String using specified charset.
 *
 * This method is not recommended on huge files.
 */
public fun File.readText(charset: String): String = readBytes().toString(charset)

/**
 * Reads the entire content of the file as a String using UTF-8 or specified charset.
 *
 * This method is not recommended on huge files.
 */
public fun File.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Writes the text as the contents of the file using specified charset.
 */
public fun File.writeText(text: String, charset: String): Unit {
    writeBytes(text.toByteArray(charset))
}

/**
 * Writes the text as the contents of the file using UTF-8 or specified charset.
 */
public fun File.writeText(text: String, charset: Charset = Charsets.UTF_8): Unit {
    writeBytes(text.toByteArray(charset))
}

/**
 * Appends text to the contents of the file using UTF-8 or specified charset.
 */
public fun File.appendText(text: String, charset: Charset = Charsets.UTF_8): Unit {
    appendBytes(text.toByteArray(charset))
}

/**
 * Appends text to the contents of the file using specified charset.
 */
public fun File.appendText(text: String, charset: String): Unit {
    appendBytes(text.toByteArray(charset))
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
 * Reads file by byte blocks and calls closure for each block read. Block size depends on implementation but never less than 512.
 * This functions passes byte array and amount of bytes in this buffer to the closure function.
 *
 * You can use this function for huge files
 */
public fun File.forEachBlock(closure: (ByteArray, Int) -> Unit): Unit {
    val arr = ByteArray(4096)
    val fis = FileInputStream(this)

    try {
        do {
            val size = fis.read(arr)
            if (size == -1) {
                break
            } else if (size > 0) {
                closure(arr, size)
            }
        } while (true)
    } finally {
        fis.close()
    }
}

/**
 * Reads file line by line using specified [charset]. Default charset is UTF-8.
 *
 * You may use this function on huge files
 */
public fun File.forEachLine(charset: Charset = Charsets.UTF_8, closure: (line: String) -> Unit): Unit {
    val reader = BufferedReader(InputStreamReader(FileInputStream(this), charset))
    try {
        reader.forEachLine(closure)
    } finally {
        reader.close()
    }
}

/**
 * Reads file line by line using the specified [charset].
 *
 * You may use this function on huge files
 */
public fun File.forEachLine(charset: String, closure: (line: String) -> Unit): Unit = forEachLine(Charset.forName(charset), closure)

/**
 * Reads file content into list of lines using specified [charset]
 *
 * Do not use this function for huge files.
 */
public fun File.readLines(charset: String): List<String> = readLines(Charset.forName(charset))

/**
 * Reads file content as strings list. By default uses UTF-8 charset.
 *
 * Do not use this function for huge files.
 */
public fun File.readLines(charset: Charset = Charsets.UTF_8): List<String> {
    val result = ArrayList<String>()
    forEachLine(charset) { result.add(it); }
    return result
}

/**
 * Returns an array of files and directories in the directory that satisfy the specified filter.
 */
public fun File.listFiles(filter: (file: File) -> Boolean): Array<File>? = listFiles(
        object : FileFilter {
            override fun accept(file: File) = filter(file)
        }
                                                                                    )
