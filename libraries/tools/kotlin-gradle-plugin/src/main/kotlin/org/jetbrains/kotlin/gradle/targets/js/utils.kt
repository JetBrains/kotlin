/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File
import java.security.MessageDigest

fun Appendable.appendConfigsFromDir(confDir: File) {
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

fun ByteArray.toHex(): String {
    val result = CharArray(size * 2) { ' ' }
    var i = 0
    forEach {
        val n = it.toInt()
        result[i++] = Character.forDigit(n shr 4 and 0xF, 16)
        result[i++] = Character.forDigit(n and 0xF, 16)
    }
    return String(result)
}

fun calculateDirHash(dir: File): String? {
    if (!dir.isDirectory) return null
    val md = MessageDigest.getInstance("MD5")
    dir.walk()
        .forEach { file ->
            md.update(file.absolutePath.toByteArray())
            if (file.isFile) {
                file.inputStream().use {
                    md.update(it.readBytes())
                }
            }
        }

    val digest = md.digest()

    return digest.toHex()
}

const val JS = "js"
const val JS_MAP = "js.map"
const val META_JS = "meta.js"
