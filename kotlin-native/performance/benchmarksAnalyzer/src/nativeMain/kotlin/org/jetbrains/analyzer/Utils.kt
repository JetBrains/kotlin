/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.analyzer

import platform.posix.*
import kotlinx.cinterop.*

actual fun readFile(fileName: String): String {
    val file = fopen(fileName, "r") ?: error("Cannot read file '$fileName'")
    var buffer = ByteArray(1024)
    var text = StringBuilder()
    try {
        while (true) {
            val nextLine = fgets(buffer.refTo(0), buffer.size, file)?.toKString()
            if (nextLine == null) break
            text.append(nextLine)
        }
    } finally {
        fclose(file)
    }
    return text.toString()
}

actual fun Double.format(decimalNumber: Int): String {
    var buffer = ByteArray(1024)
    snprintf(buffer.refTo(0), buffer.size.toULong(), "%.${decimalNumber}f", this)
    return buffer.toKString()
}

actual fun writeToFile(fileName: String, text: String) {
    val file = fopen(fileName, "wt") ?: error("Cannot write file '$fileName'")
    try {
        if (fputs(text, file) == EOF) throw Error("File write error")
    } finally {
        fclose(file)
    }
}