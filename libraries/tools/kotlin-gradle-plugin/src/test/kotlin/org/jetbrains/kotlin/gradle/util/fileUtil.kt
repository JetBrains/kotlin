package org.jetbrains.kotlin.gradle.util

import java.io.File

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

