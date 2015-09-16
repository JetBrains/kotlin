@file:JvmVersion
@file:JvmMultifileClass
@file:JvmName("FilesKt")
package kotlin.io

import java.io.*
import java.util.*
import java.nio.charset.Charset


/**
 * Returns a new [FileReader] for reading the content of this file.
 */
public fun File.reader(): FileReader = FileReader(this)

/**
 * Returns a new [BufferedReader] for reading the content of this file.
 *
 * @param bufferSize necessary size of the buffer.
 */
public fun File.bufferedReader(bufferSize: Int = defaultBufferSize): BufferedReader = reader().buffered(bufferSize)

/**
 * Returns a new [FileWriter] for writing the content of this file.
 */
public fun File.writer(): FileWriter = FileWriter(this)

/**
 * Returns a new [BufferedWriter] for writing the content of this file.
 *
 * @param bufferSize necessary size of the buffer.
 */
public fun File.bufferedWriter(bufferSize: Int = defaultBufferSize): BufferedWriter = writer().buffered(bufferSize)

/**
 * Returns a new [PrintWriter] for writing the content of this file.
 */
public fun File.printWriter(): PrintWriter = PrintWriter(bufferedWriter())

/**
 * Gets the entire content of this file as a byte array.
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB byte array size.
 *
 * @return the entire content of this file as a byte array.
 */
public fun File.readBytes(): ByteArray = FileInputStream(this).use { it.readBytes(length().toInt()) }

/**
 * Sets the content of this file as an [array] of bytes.
 * If this file already exists, it becomes overwritten.
 *
 * @param array byte array to write into this file.
 */
public fun File.writeBytes(array: ByteArray): Unit = FileOutputStream(this).use { it.write(array) }

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
public fun File.appendBytes(array: ByteArray): Unit = FileOutputStream(this, true).use { it.write(array) }

/**
 * Gets the entire content of this file as a String using specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
 *
 * @param charset character set to use.
 * @return the entire content of this file as a String.
 */
public fun File.readText(charset: String): String = readBytes().toString(charset)

/**
 * Gets the entire content of this file as a String using UTF-8 or specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
 *
 * @param charset character set to use.
 * @return the entire content of this file as a String.
 */
public fun File.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Sets the content of this file as [text] encoded using the specified [charset].
 * If this file exists, it becomes overwritten.
 *
 * @param text text to write into file.
 * @param charset character set to use.
 */
public fun File.writeText(text: String, charset: String): Unit = writeBytes(text.toByteArray(charset))

/**
 * Sets the content of this file as [text] encoded using UTF-8 or specified [charset].
 * If this file exists, it becomes overwritten.
 *
 * @param text text to write into file.
 * @param charset character set to use.
 */
public fun File.writeText(text: String, charset: Charset = Charsets.UTF_8): Unit = writeBytes(text.toByteArray(charset))

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use.
 */
public fun File.appendText(text: String, charset: Charset = Charsets.UTF_8): Unit = appendBytes(text.toByteArray(charset))

/**
 * Appends [text] to the content of the file using the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use.
 */
public fun File.appendText(text: String, charset: String): Unit = appendBytes(text.toByteArray(charset))

/**
 * Reads file by byte blocks and calls [operation] for each block read.
 * Block has default size which is implementation-dependent.
 * This functions passes the byte array and amount of bytes in the array to the [operation] function.
 *
 * You can use this function for huge files.
 *
 * @param operation function to process file blocks.
 */
public fun File.forEachBlock(operation: (ByteArray, Int) -> Unit): Unit = forEachBlock(operation, defaultBlockSize)

/**
 * Reads file by byte blocks and calls [operation] for each block read.
 * This functions passes the byte array and amount of bytes in the array to the [operation] function.
 *
 * You can use this function for huge files.
 *
 * @param operation function to process file blocks.
 * @param blockSize size of a block, replaced by 512 if it's less, 4096 by default.
 */
public fun File.forEachBlock(operation: (ByteArray, Int) -> Unit, blockSize: Int): Unit {
    val arr = ByteArray(if (blockSize < minimumBlockSize) minimumBlockSize else blockSize)
    val fis = FileInputStream(this)

    try {
        do {
            val size = fis.read(arr)
            if (size <= 0) {
                break
            } else {
                operation(arr, size)
            }
        } while (true)
    } finally {
        fis.close()
    }
}

/**
 * Reads this file line by line using the specified [charset] and calls [operation] for each line.
 * Default charset is UTF-8.
 *
 * You may use this function on huge files.
 *
 * @param charset character set to use.
 * @param operation function to process file lines.
 */
public fun File.forEachLine(charset: Charset = Charsets.UTF_8, operation: (line: String) -> Unit): Unit {
    // Note: close is called at forEachLine
    BufferedReader(InputStreamReader(FileInputStream(this), charset)).forEachLine(operation)
}

/**
 * Reads this file line by line using the specified [charset] and calls [operation] for each line.
 *
 * You may use this function on huge files.
 *
 * @param charset character set to use.
 * @param operation function to process file lines.
 */
public fun File.forEachLine(charset: String, operation: (line: String) -> Unit): Unit = forEachLine(Charset.forName(charset), operation)

/**
 * Reads the file content as a list of lines, using the specified [charset].
 *
 * Do not use this function for huge files.
 *
 * @param charset character set to use.
 * @return list of file lines.
 */
public fun File.readLines(charset: String): List<String> = readLines(Charset.forName(charset))

/**
 * Constructs a new FileInputStream of this file and returns it as a result.
 */
public fun File.inputStream(): InputStream {
    return FileInputStream(this)
}

/**
 * Constructs a new FileOutputStream of this file and returns it as a result.
 */
public fun File.outputStream(): OutputStream {
    return FileOutputStream(this)
}

/**
 * Reads the file content as a list of lines. By default uses UTF-8 charset.
 *
 * Do not use this function for huge files.
 *
 * @param charset character set to use.
 * @return list of file lines.
 */
public fun File.readLines(charset: Charset = Charsets.UTF_8): List<String> {
    val result = ArrayList<String>()
    forEachLine(charset) { result.add(it); }
    return result
}

