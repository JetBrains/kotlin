package kotlin.io

import java.io.*
import java.nio.charset.*
import java.util.NoSuchElementException
import java.net.URL


/**
 * Recursively process this file and all children with the given block
 */
public fun File.recurse(block: (File) -> Unit): Unit {
    block(this)
    if (this.isDirectory()) {
        for (child in this.listFiles()) {
            if (child != null) {
                child.recurse(block)
            }
        }
    }
}

/**
 * Returns this if the file is a directory or the parent if its a file inside a directory
 */
inline val File.directory: File
get() = if (this.isDirectory()) this else this.getParentFile().sure()

/**
 * Returns the canoncial path of the file
 */
inline val File.canonicalPath: String
get() = getCanonicalPath() ?: ""

/**
 * Returns the file name or "" for an empty name
 */
inline val File.name: String
get() = getName() ?: ""

/**
 * Returns the file path or "" for an empty name
 */
inline val File.path: String
get() = getPath() ?: ""

/**
 * Returns true if the file ends with the given extension
 */
inline val File.extension: String
get() {
    val text = this.name
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
    return file.directory.canonicalPath.startsWith(this.directory.canonicalPath)
}

/**
 * Returns the relative path of the given descendant of this file if its a descendant
 */
public fun File.relativePath(descendant: File): String {
    val prefix = this.directory.canonicalPath
    val answer = descendant.canonicalPath
    return if (answer.startsWith(prefix)) {
        answer.substring(prefix.size + 1)
    } else {
        answer
    }
}

/**
 * Creates a new [[FileReader]] for this file
 */
public inline fun File.reader(): FileReader = FileReader(this)

/**
 * Iterates through each line of this file then closing the underlying [[Reader]] when its completed
 */
public inline fun File.forEachLine(block: (String) -> Any): Unit = reader().forEachLine(block)

/**
 * Reads the entire content of the file as bytes
 *
 * This method is not recommended on huge files.
 */
public fun File.readBytes(): ByteArray {
    return FileInputStream(this).use<FileInputStream,ByteArray>{ it.readBytes(this.length().toInt()) }
}

/**
 * Writes the bytes as the contents of the file
 */
public fun File.writeBytes(data: ByteArray): Unit {
    return FileOutputStream(this).use<FileOutputStream,Unit>{ it.write(data) }
}
/**
 * Reads the entire content of the file as a String using the optional
 * character encoding.  The default platform encoding is used if the character
 * encoding is not specified or null.
 *
 * This method is not recommended on huge files.
 */
public fun File.readText(encoding:String? = null) : String = readBytes().toString(encoding)

/**
 * Reads the entire content of the file as a String using the
 * character encoding.
 *
 * This method is not recommended on huge files.
 */
public fun File.readText(encoding:Charset) : String = readBytes().toString(encoding)

/**
 * Writes the text as the contents of the file using the optional
 * character encoding.  The default platform encoding is used if the character
 * encoding is not specified or null.
 */
public fun File.writeText(text: String, encoding:String?=null): Unit { writeBytes(text.toByteArray(encoding)) }

/**
 * Writes the text as the contents of the file using the optional
 * character encoding.  The default platform encoding is used if the character
 * encoding is not specified or null.
 */
public fun File.writeText(text: String, encoding:Charset): Unit { writeBytes(text.toByteArray(encoding)) }

/**
 * Copies this file to the given output file, returning the number of bytes copied
 */
public fun File.copyTo(file: File, bufferSize: Int = defaultBufferSize): Long {
    file.directory.mkdirs()
    val input = FileInputStream(this)
    return input.use<FileInputStream,Long>{
        val output = FileOutputStream(file)
        output.use<FileOutputStream,Long>{
            input.copyTo(output, bufferSize)
        }
    }
}
