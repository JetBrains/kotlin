/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js.ast

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.nio.charset.Charset

abstract class AbstractSerializer {
    protected open val charset: Charset get() = Charsets.UTF_8
    private val stringMap = mutableMapOf<String, Int>()
    private val stringSerializer = DataWriter()

    fun internalizeString(string: String): Int = stringMap.getOrPut(string) {
        stringSerializer.writeString(string)
        stringMap.size
    }

    protected abstract fun DataOutputStream.serialize()

    fun saveTo(rawOutput: OutputStream) {
        DataOutputStream(rawOutput).use {
            it.writeInt(stringMap.size)
            stringSerializer.saveTo(it)
            it.serialize()
        }
    }

    inner class DataWriter {
        val data = ByteArrayOutputStream()

        @PublishedApi
        internal val output = DataOutputStream(data)

        fun saveTo(output: DataOutputStream) {
            data.writeTo(output)
        }

        fun writeByte(byte: Int) {
            // Limit bytes to positive values to avoid conversion in deserializer
            if ((byte and 0x7F.inv()) != 0) error("Byte out of bounds: $byte")
            output.writeByte(byte)
        }

        fun writeByteArray(byteArray: ByteArray) {
            output.writeInt(byteArray.size)
            output.write(byteArray)
        }

        fun writeString(string: String) {
            writeByteArray(string.toByteArray(charset))
        }

        fun writeBoolean(boolean: Boolean) {
            output.writeBoolean(boolean)
        }

        fun writeInt(int: Int) {
            output.writeInt(int)
        }

        fun writeDouble(double: Double) {
            output.writeDouble(double)
        }

        inline fun <T> writeCollection(collection: Collection<T>, writeItem: (T) -> Unit) {
            output.writeInt(collection.size)
            collection.forEach(writeItem)
        }

        inline fun <T> ifNotNull(t: T?, write: (T) -> Unit): T? {
            output.writeBoolean(t != null)
            if (t != null) {
                write(t)
            }
            return t
        }

        inline fun ifTrue(condition: Boolean, write: () -> Unit) {
            output.writeBoolean(condition)
            if (condition) {
                write()
            }
        }
    }
}
