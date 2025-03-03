/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import java.io.File
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun File.allJavaFiles(): Iterable<File> =
    allFilesWithExtension("java")

fun File.allFilesWithExtension(ext: String): Iterable<File> =
    walk()
        .filter { it.isFile && it.extension.equals(ext, ignoreCase = true) }
        .toList()

fun Path.replaceText(oldValue: String, newValue: String) {
    writeText(readText().replace(oldValue, newValue))
}

fun File.replaceText(oldValue: String, newValue: String) {
    writeText(readText().replace(oldValue, newValue))
}

fun Path.replaceFirst(oldValue: String, newValue: String) {
    writeText(readText().replaceFirst(oldValue, newValue))
}

fun Path.replaceWithVersion(versionSuffix: String): Path {
    val otherVersion = resolveSibling("$fileName.$versionSuffix")
    otherVersion.copyTo(this, overwrite = true)
    return this
}
