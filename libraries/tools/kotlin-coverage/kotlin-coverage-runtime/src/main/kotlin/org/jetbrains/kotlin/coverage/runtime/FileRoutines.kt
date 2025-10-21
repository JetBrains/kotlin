/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage.runtime

import java.io.File
import java.io.OutputStream
import kotlin.experimental.or

// expect-actual
public object FileRoutines {
    public fun writeToFile(filePath: String, block: Writer.() -> Unit) {
        val file = File(filePath)
        file.parentFile.mkdirs()
        file.outputStream().buffered().use {
            WriterActual(it).block()
        }
    }


}

// expect
public interface Writer {
    public fun writeInt(value: Int)
    public fun writeBooleanArray(array: BooleanArray)
}

// actual
private class WriterActual(val output: OutputStream) : Writer {
    override fun writeInt(value: Int) {
        output.write(value and 0xFF000000.toInt() shr 24)
        output.write(value and 0xFF0000 shr 16)
        output.write(value and 0xFF00 shr 8)
        output.write(value and 0xFF)
    }

    override fun writeBooleanArray(array: BooleanArray) {
        val bytesSize = array.size / 8 + if (array.size % 8 > 0) 1 else 0
        val result = ByteArray(bytesSize) { 0 }
        array.forEachIndexed { index, value ->
            val byteIndex = index / 8
            val bitIndex = index % 8
            if (value) {
                result[byteIndex] = result[byteIndex] or (1 shl bitIndex).toByte()
            }
        }
        output.write(result)
    }
}

