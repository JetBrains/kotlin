package org.jetbrains.kotlin.gradle.util

import java.io.File
import java.nio.file.Files

fun File.getFileByName(name: String): File =
        findFileByName(name) ?: throw AssertionError("Could not find file with name '$name' in $this")

fun File.getFilesByNames(vararg names: String): List<File> =
        names.map { getFileByName(it) }

fun File.findFileByName(name: String): File? =
        walk().filter { it.isFile && it.name.equals(name, ignoreCase = true) }.firstOrNull()

fun File.allKotlinFiles(): Iterable<File> =
        allFilesWithExtension("kt")

fun File.allJavaFiles(): Iterable<File> =
        allFilesWithExtension("java")

fun File.allFilesWithExtension(ext: String): Iterable<File> =
        walk().filter { it.isFile && it.extension.equals(ext, ignoreCase = true) }.toList()

fun File.modify(transform: (String)->String) {
    writeText(transform(readText()))
}

fun createTempDir(prefix: String): File =
    Files.createTempDirectory(prefix).toFile().apply {
        deleteOnExit()
    }

/**
 * converts back slashes to forward slashes
 * removes double slashes inside the path, e.g. "x/y//z" => "x/y/z"
 *
 * Converted from com.intellij.openapi.util.io.FileUtil.normalize
 */
fun normalizePath(path: String): String {
    var start = 0
    var separator = false
    if (isWindows) {
        if (path.startsWith("//")) {
            start = 2
            separator = true
        } else if (path.startsWith("\\\\")) {
            return normalizeTail(0, path, false)
        }
    }

    for (i in start until path.length) {
        val c = path[i]
        if (c == '/') {
            if (separator) {
                return normalizeTail(i, path, true)
            }
            separator = true
        } else if (c == '\\') {
            return normalizeTail(i, path, separator)
        } else {
            separator = false
        }
    }

    return path
}

private fun normalizeTail(prefixEnd: Int, path: String, separator: Boolean): String {
    var separator = separator
    val result = StringBuilder(path.length)
    result.append(path, 0, prefixEnd)
    var start = prefixEnd
    if (start == 0 && isWindows && (path.startsWith("//") || path.startsWith("\\\\"))) {
        start = 2
        result.append("//")
        separator = true
    }

    for (i in start until path.length) {
        val c = path[i]
        if (c == '/' || c == '\\') {
            if (!separator) result.append('/')
            separator = true
        } else {
            result.append(c)
            separator = false
        }
    }

    return result.toString()
}