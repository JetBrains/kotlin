package kotlin.io

import java.io.*
import java.nio.charset.*
import java.util.NoSuchElementException
import java.util.ArrayList
import java.net.URL


/**
 * Recursively process this file and all children with the given block
 */
public fun File.recurse(block: (File) -> Unit): Unit {
    block(this)
    val children = this.listFiles()
    if (children != null) {
        for (child in children) {
            child.recurse(block)
        }
    }
}

/**
 * Returns this if the file is a directory or the parent if its a file inside a directory
 */
inline val File.directory: File
get() = if (this.isDirectory()) this else this.getParentFile()!!

/**
 * Returns the canonical path of the file
 */
inline val File.canonicalPath: String
get() = getCanonicalPath()

/**
 * Returns the file name or "" for an empty name
 */
inline val File.name: String
get() = getName()

/**
 * Returns the file path or "" for an empty name
 */
inline val File.path: String
get() = getPath()

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
public inline fun File.reader(): FileReader = FileReader(this)

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

/**
 * Reads file by byte blocks and calls closure for each block read. Block size depends on implementation but never less than 512.
 * This functions passes byte array and amount of bytes in this buffer to the closure function.
 *
 * You can use this function for huge files
 */
fun File.forEachBlock(closure : (ByteArray, Int) -> Unit) : Unit {
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
        } while(true)
    } finally {
        fis.close()
    }
}

/**
 * Reads file line by line. Default charset is UTF-8.
 *
 * You may use this function on huge files
 */
fun File.forEachLine (charset : String = "UTF-8", closure : (line : String) -> Unit) : Unit {
    val reader = BufferedReader(InputStreamReader(FileInputStream(this), charset))
    try {
        reader.forEachLine(closure)
    } finally {
        reader.close()
    }
}

/**
 * Reads file content as strings list. By default uses UTF-8 charset.
 *
 * Do not use this function for huge files.
 */
fun File.readLines(charset : String = "UTF-8") : List<String> {
    val rs = ArrayList<String>()

    this.forEachLine(charset) { (line : String) : Unit ->
        rs.add(line);
    }

    return rs
}

/**
 * Returns an array of files and directories in the directory that satisfy the specified filter.
 */
fun File.listFiles(filter : (file : File) -> Boolean) : Array<File>? = listFiles(
    object : FileFilter {
        override fun accept(file: File) = filter(file)
    }
)
