/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.incremental.storage

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import gnu.trove.THashSet
import gnu.trove.TIntHashSet
import gnu.trove.decorator.TIntHashSetDecorator
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.File
import java.util.*

object INT_PAIR_KEY_DESCRIPTOR : KeyDescriptor<IntPair> {
    override fun read(`in`: DataInput): IntPair {
        val first = `in`.readInt()
        val second = `in`.readInt()
        return IntPair(first, second)
    }

    override fun save(out: DataOutput, value: IntPair?) {
        if (value == null) return

        out.writeInt(value.first)
        out.writeInt(value.second)
    }

    override fun getHashCode(value: IntPair?): Int = value?.hashCode() ?: 0

    override fun isEqual(val1: IntPair?, val2: IntPair?): Boolean = val1 == val2
}


object PATH_FUNCTION_PAIR_KEY_DESCRIPTOR : KeyDescriptor<PathFunctionPair> {
    override fun getHashCode(value: PathFunctionPair): Int =
            value.hashCode()

    override fun isEqual(val1: PathFunctionPair, val2: PathFunctionPair): Boolean =
            val1 == val2

    override fun read(`in`: DataInput): PathFunctionPair {
        val path = IOUtil.readUTF(`in`)
        val function = IOUtil.readUTF(`in`)
        return PathFunctionPair(path, function)
    }

    override fun save(out: DataOutput, value: PathFunctionPair) {
        IOUtil.writeUTF(out, value.path)
        IOUtil.writeUTF(out, value.function)
    }
}


object PROTO_MAP_VALUE_EXTERNALIZER : DataExternalizer<ProtoMapValue> {
    override fun save(out: DataOutput, value: ProtoMapValue) {
        out.writeBoolean(value.isPackageFacade)
        out.writeInt(value.bytes.size())
        out.write(value.bytes)
        out.writeInt(value.strings.size())
        for (string in value.strings) {
            out.writeUTF(string)
        }
    }

    override fun read(`in`: DataInput): ProtoMapValue {
        val isPackageFacade = `in`.readBoolean()
        val bytesLength = `in`.readInt()
        val bytes = ByteArray(bytesLength)
        `in`.readFully(bytes, 0, bytesLength)
        val stringsLength = `in`.readInt()
        val strings = Array<String>(stringsLength) { `in`.readUTF() }
        return ProtoMapValue(isPackageFacade, bytes, strings)
    }
}


abstract class StringMapExternalizer<T> : DataExternalizer<Map<String, T>> {
    override fun save(out: DataOutput, map: Map<String, T>?) {
        out.writeInt(map!!.size())

        for ((key, value) in map.entrySet()) {
            IOUtil.writeString(key, out)
            writeValue(out, value)
        }
    }

    override fun read(`in`: DataInput): Map<String, T>? {
        val size = `in`.readInt()
        val map = HashMap<String, T>(size)

        repeat(size) {
            val name = IOUtil.readString(`in`)!!
            map[name] = readValue(`in`)
        }

        return map
    }

    protected abstract fun writeValue(output: DataOutput, value: T)
    protected abstract fun readValue(input: DataInput): T
}


object STRING_TO_LONG_MAP_EXTERNALIZER : StringMapExternalizer<Long>() {
    override fun readValue(input: DataInput): Long =
            input.readLong()

    override fun writeValue(output: DataOutput, value: Long) {
        output.writeLong(value)
    }
}


object STRING_LIST_EXTERNALIZER : DataExternalizer<List<String>> {
    override fun save(out: DataOutput, value: List<String>) {
        value.forEach { IOUtil.writeUTF(out, it) }
    }

    override fun read(`in`: DataInput): List<String> {
        val result = ArrayList<String>()
        while ((`in` as DataInputStream).available() > 0) {
            result.add(IOUtil.readUTF(`in`))
        }
        return result
    }
}


object PATH_COLLECTION_EXTERNALIZER : DataExternalizer<Collection<String>> {
    override fun save(out: DataOutput, value: Collection<String>) {
        for (str in value) {
            IOUtil.writeUTF(out, str)
        }
    }

    override fun read(`in`: DataInput): Collection<String> {
        val result = THashSet(FileUtil.PATH_HASHING_STRATEGY)
        val stream = `in` as DataInputStream
        while (stream.available() > 0) {
            val str = IOUtil.readUTF(stream)
            result.add(str)
        }
        return result
    }
}

object CONSTANTS_MAP_EXTERNALIZER : DataExternalizer<Map<String, Any>> {
    override fun save(out: DataOutput, map: Map<String, Any>?) {
        out.writeInt(map!!.size())
        for (name in map.keySet().sorted()) {
            IOUtil.writeString(name, out)
            val value = map[name]!!
            when (value) {
                is Int -> {
                    out.writeByte(Kind.INT.ordinal())
                    out.writeInt(value)
                }
                is Float -> {
                    out.writeByte(Kind.FLOAT.ordinal())
                    out.writeFloat(value)
                }
                is Long -> {
                    out.writeByte(Kind.LONG.ordinal())
                    out.writeLong(value)
                }
                is Double -> {
                    out.writeByte(Kind.DOUBLE.ordinal())
                    out.writeDouble(value)
                }
                is String -> {
                    out.writeByte(Kind.STRING.ordinal())
                    IOUtil.writeString(value, out)
                }
                else -> throw IllegalStateException("Unexpected constant class: ${value.javaClass}")
            }
        }
    }

    override fun read(`in`: DataInput): Map<String, Any>? {
        val size = `in`.readInt()
        val map = HashMap<String, Any>(size)

        repeat(size) {
            val name = IOUtil.readString(`in`)!!

            val kind = Kind.values()[`in`.readByte().toInt()]
            val value = when (kind) {
                Kind.INT -> `in`.readInt()
                Kind.FLOAT -> `in`.readFloat()
                Kind.LONG -> `in`.readLong()
                Kind.DOUBLE -> `in`.readDouble()
                Kind.STRING -> IOUtil.readString(`in`)!!
            }
            map[name] = value
        }

        return map
    }

    private enum class Kind {
        INT, FLOAT, LONG, DOUBLE, STRING
    }
}


object INT_SET_EXTERNALIZER : DataExternalizer<Set<Int>> {
    override fun save(out: DataOutput, value: Set<Int>) {
        value.forEach { out.writeInt(it) }
    }

    override fun read(`in`: DataInput): Set<Int> {
        val result = TIntHashSet()
        val stream = `in` as DataInputStream

        while (stream.available() > 0) {
            val str = stream.readInt()
            result.add(str)
        }

        return TIntHashSetDecorator(result)
    }
}

object INT_EXTERNALIZER : DataExternalizer<Int> {
    override fun read(`in`: DataInput): Int = `in`.readInt()

    override fun save(out: DataOutput, value: Int) {
        out.writeInt(value)
    }
}

object FILE_EXTERNALIZER : DataExternalizer<File> {
    override fun read(`in`: DataInput): File = File(`in`.readUTF())

    override fun save(out: DataOutput, value: File) {
        out.writeUTF(value.canonicalPath)
    }
}

object FILE_KEY_DESCRIPTOR : KeyDescriptor<File> {
    override fun read(`in`: DataInput): File = File(`in`.readUTF())

    override fun save(out: DataOutput, value: File) {
        out.writeUTF(value.canonicalPath)
    }

    override fun getHashCode(value: File?): Int =
            FileUtil.FILE_HASHING_STRATEGY.computeHashCode(value)

    override fun isEqual(val1: File?, val2: File?): Boolean =
            FileUtil.FILE_HASHING_STRATEGY.equals(val1, val2)
}