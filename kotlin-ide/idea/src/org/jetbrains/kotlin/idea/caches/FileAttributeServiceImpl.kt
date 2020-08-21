/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.util.io.DataInputOutputUtil
import java.io.DataInput
import java.io.DataOutput
import java.util.concurrent.ConcurrentHashMap

class FileAttributeServiceImpl : FileAttributeService {
    val attributes: MutableMap<String, FileAttribute> = ConcurrentHashMap()

    override fun register(id: String, version: Int, fixedSize: Boolean) {
        attributes[id] = FileAttribute(id, version, fixedSize)
    }

    override fun <T : Enum<T>> writeEnumAttribute(id: String, file: VirtualFile, value: T): CachedAttributeData<T> =
        write(file, id, value) { output, v ->
            DataInputOutputUtil.writeINT(output, v.ordinal)
        }

    override fun <T : Enum<T>> readEnumAttribute(id: String, file: VirtualFile, klass: Class<T>): CachedAttributeData<T>? =
        read(file, id) { input ->
            deserializeEnumValue(DataInputOutputUtil.readINT(input), klass)
        }


    override fun writeBooleanAttribute(id: String, file: VirtualFile, value: Boolean): CachedAttributeData<Boolean> =
        write(file, id, value) { output, v ->
            DataInputOutputUtil.writeINT(output, if (v) 1 else 0)
        }

    override fun readBooleanAttribute(id: String, file: VirtualFile): CachedAttributeData<Boolean>? = read(file, id) { input ->
        DataInputOutputUtil.readINT(input) > 0
    }

    override fun <T> write(file: VirtualFile, id: String, value: T, writeValueFun: (DataOutput, T) -> Unit): CachedAttributeData<T> {
        val attribute = attributes[id] ?: throw IllegalArgumentException("Attribute with $id wasn't registered")

        val data = CachedAttributeData(value, timeStamp = file.timeStamp)

        attribute.writeAttribute(file).use {
            DataInputOutputUtil.writeTIME(it, data.timeStamp)
            writeValueFun(it, value)
        }

        return data
    }

    override fun <T> read(file: VirtualFile, id: String, readValueFun: (DataInput) -> T): CachedAttributeData<T>? {
        val attribute = attributes[id] ?: throw IllegalArgumentException("Attribute with $id wasn't registered")
        if (file !is VirtualFileWithId) return null
        if (!file.isValid) return null

        val stream = attribute.readAttribute(file) ?: return null
        return stream.use {
            val timeStamp = DataInputOutputUtil.readTIME(it)
            val value = readValueFun(it)

            if (file.timeStamp == timeStamp) {
                CachedAttributeData(value, timeStamp)
            } else {
                null
            }
        }
    }

    private fun <T : Enum<T>> deserializeEnumValue(i: Int, klass: Class<T>): T {
        val method = klass.getMethod("values")

        @Suppress("UNCHECKED_CAST")
        val values = method.invoke(null) as Array<T>

        return values[i]
    }
}

