/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import org.gradle.internal.hash.FileHasher
import org.gradle.internal.hash.Hashing.defaultFunction
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File
import java.nio.file.Files

internal fun Appendable.appendConfigsFromDir(confDir: File) {
    val files = confDir.listFiles() ?: return

    files.asSequence()
        .filter { it.isFile }
        .filter { it.extension == "js" }
        .sortedBy { it.name }
        .forEach {
            appendLine("// ${it.name}")
            append(it.readText())
            appendLine()
            appendLine()
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
    dir: File,
): String? {
    if (!dir.isDirectory) return null

    val hasher = defaultFunction().newHasher()
    dir.walk()
        .forEach { file ->
            hasher.putString(file.toRelativeString(dir))
            if (file.isFile) {
                if (!Files.isSymbolicLink(file.toPath())) {
                    hasher.putHash(hash(file))
                } else {
                    val absoluteFile = file.absoluteFile
                    hasher.putHash(hash(absoluteFile))
                    hasher.putString(absoluteFile.toRelativeString(dir))
                }
            }
        }

    return hasher.hash().toByteArray().toHex()
}
