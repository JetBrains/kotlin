/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import org.gradle.internal.hash.FileHasher
import org.gradle.internal.hash.Hashing.defaultFunction
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink

internal fun Appendable.appendConfigsFromDir(confDir: Path) {
    if (!confDir.isDirectory()) return

    Files.list(confDir).use { files ->
        files
            .filter { it.isRegularFile() }
            .filter { it.fileName.toString().substringAfterLast('.', "") == "js" }
            .sorted(compareBy<Path> { it.fileName.toString() })
            .forEach {
                appendLine("// ${it.fileName}")
                append(it.bufferedReader().use { reader -> reader.readText() })
                appendLine()
                appendLine()
            }
    }
}

internal fun ByteArray.toHex(): String {
    val result = CharArray(size * 2) { ' ' }
    var i = 0
    forEach {
        val n = it.toInt()
        result[i++] = Character.forDigit(n shr 4 and 0xF, 16)
        result[i++] = Character.forDigit(n and 0xF, 16)
    }
    return String(result)
}

internal fun FileHasher.calculateDirHash(
    dir: Path,
): String? {
    if (!dir.isDirectory()) return null

    val hasher = defaultFunction().newHasher()
    val baseDir = dir.toAbsolutePath()
    Files.walk(baseDir)
        .use { paths ->
            paths.forEach { file ->
                hasher.putString(baseDir.relativize(file).toString())
                if (file.isRegularFile()) {
                    if (!file.isSymbolicLink()) {
                        hasher.putHash(hash(file.toFile()))
                    } else {
                        val absoluteFile = file.toAbsolutePath()
                        hasher.putHash(hash(absoluteFile.toFile()))
                        hasher.putString(baseDir.relativize(absoluteFile).toString())
                    }
                }
            }
        }

    return hasher.hash().toByteArray().toHex()
}
