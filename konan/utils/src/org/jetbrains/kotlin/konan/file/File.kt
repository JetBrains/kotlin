/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import org.jetbrains.kotlin.konan.util.removeSuffixIfPresent
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.lang.Exception
import java.net.URI
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

data class File constructor(internal val javaPath: Path) {
    constructor(parent: Path, child: String): this(parent.resolve(child))
    constructor(parent: File, child: String): this(parent.javaPath.resolve(child))
    constructor(path: String): this(Paths.get(path))
    constructor(parent: String, child: String): this(Paths.get(parent, child))

    val path: String
        get() = javaPath.toString()
    val absolutePath: String
        get() = javaPath.toAbsolutePath().toString()
    val absoluteFile: File
        get() = File(absolutePath)
    val name: String
        get() = javaPath.fileName.toString().removeSuffixIfPresent("/") // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8153248
    val extension: String
        get() = name.substringAfterLast('.', "")
    val parent: String
        get() = javaPath.parent.toString()
    val parentFile: File
        get() = File(javaPath.parent)

    val exists
        get() = Files.exists(javaPath)
    val isDirectory
        get() = Files.isDirectory(javaPath)
    val isFile
        get() = Files.isRegularFile(javaPath)
    val isAbsolute
        get() = javaPath.isAbsolute()
    val listFiles: List<File>
        get() = Files.newDirectoryStream(javaPath).use { stream -> stream.map { File(it) } }
    val listFilesOrEmpty: List<File>
        get() = if (exists) listFiles else emptyList()

    fun child(name: String) = File(this, name)

    fun copyTo(destination: File) {
        Files.copy(javaPath, destination.javaPath, StandardCopyOption.REPLACE_EXISTING)
    }

    fun recursiveCopyTo(destination: File) {
        val sourcePath = javaPath
        val destPath = destination.javaPath
        sourcePath.recursiveCopyTo(destPath)
    }

    fun mkdirs() = Files.createDirectories(javaPath)
    fun delete() = Files.deleteIfExists(javaPath)
    fun deleteRecursively() = postorder{Files.delete(it)}
    fun deleteOnExitRecursively() = preorder{File(it).deleteOnExit()}

    fun preorder(task: (Path) -> Unit) {
        if (!this.exists) return

        Files.walkFileTree(javaPath, object: SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                task(file!!)
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                task(dir!!)
                return FileVisitResult.CONTINUE
            }
        })

    }

    fun postorder(task: (Path) -> Unit) {
        if (!this.exists) return

        Files.walkFileTree(javaPath, object: SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                task(file!!)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path?, exc: java.io.IOException?): FileVisitResult {
                task(dir!!)
                return FileVisitResult.CONTINUE
            }
        })
    }

    fun map(mode: FileChannel.MapMode = FileChannel.MapMode.READ_ONLY,
            start: Long = 0, size: Long = -1): MappedByteBuffer {
        val file = RandomAccessFile(path,
                                    if (mode == FileChannel.MapMode.READ_ONLY) "r" else "rw")
        val fileSize = if (mode == FileChannel.MapMode.READ_ONLY)
            file.length() else size.also { assert(size != -1L) }
        return file.channel.map(mode, start, fileSize) // Shall we .also { file.close() }?
    }

    fun deleteOnExit(): File {
        // Works only on the default file system, 
        // but that's okay for now.
        javaPath.toFile().deleteOnExit()
        return this // Allow streaming.
    }
    fun readBytes() = Files.readAllBytes(javaPath)
    fun writeBytes(bytes: ByteArray) = Files.write(javaPath, bytes)
    fun appendBytes(bytes: ByteArray)
            = Files.write(javaPath, bytes, StandardOpenOption.APPEND)

    fun writeLines(lines: Iterable<String>) {
        Files.write(javaPath, lines)
    }

    fun writeText(text: String): Unit = writeLines(listOf(text))

    fun forEachLine(action: (String) -> Unit) {
        Files.lines(javaPath).use { lines ->
            lines.forEach { action(it) }
        }
    }

    fun createAsSymlink(target: String) {
        val targetPath = Paths.get(target)
        if (Files.isSymbolicLink(this.javaPath) && Files.readSymbolicLink(javaPath) == targetPath) {
            return
        }
        Files.createSymbolicLink(this.javaPath, targetPath)
    }

    override fun toString() = path

    // TODO: Consider removeing these after konanazing java.util.Properties.
    fun bufferedReader() = Files.newBufferedReader(javaPath)
    fun outputStream() = Files.newOutputStream(javaPath)
    fun printWriter() = javaPath.toFile().printWriter()

    companion object {
        val userDir
            get() = File(System.getProperty("user.dir"))

        val userHome
            get() = File(System.getProperty("user.home"))

        val javaHome
            get() = File(System.getProperty("java.home"))
        val pathSeparator = java.io.File.pathSeparator
        val separator = java.io.File.separator
    }

    fun readStrings() = mutableListOf<String>().also { list -> forEachLine{list.add(it)}}

    override fun equals(other: Any?): Boolean {
        val otherFile = other as? File ?: return false
        return otherFile.javaPath.toAbsolutePath() == javaPath.toAbsolutePath()
    }

    override fun hashCode() = javaPath.toAbsolutePath().hashCode()
}

fun String.File(): File = File(this)
fun Path.File(): File = File(this)

fun createTempFile(name: String, suffix: String? = null)
        = Files.createTempFile(name, suffix).File()
fun createTempDir(name: String): File
        = Files.createTempDirectory(name).File()

fun Path.recursiveCopyTo(destPath: Path) {
    val sourcePath = this
    Files.walk(sourcePath).forEach next@ { oldPath ->

        val relative = sourcePath.relativize(oldPath)
        val destFs = destPath.getFileSystem()
        // We are copying files between file systems, 
        // so pass the relative path through the String.
        val newPath = destFs.getPath(destPath.toString(), relative.toString())

        // File systems don't allow replacing an existing root.
        if (newPath == newPath.getRoot()) return@next
        if (Files.isDirectory(newPath)) {
            Files.createDirectories(newPath)
        } else {
            Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

fun bufferedReader(errorStream: InputStream?) = BufferedReader(InputStreamReader(errorStream))

// stdlib `use` function adapted for AutoCloseable.
inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this?.close()
        } catch (closeException: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            this?.close()
        }
    }
}
