package kdump

import base.Endianness
import base.nullUnless
import io.*
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream

data class Reader(
        val inputStream: InputStream,
        val endianness: Endianness,
        val idSize: IdSize,
)

fun Reader.readByte(): Byte =
        inputStream.readByte()

fun Reader.readByteInt(): Int =
        inputStream.readByteInt()

fun Reader.readShort(): Short =
        inputStream.readShort(endianness)

fun Reader.readInt(): Int =
        inputStream.readInt(endianness)

fun Reader.readLong(): Long =
        inputStream.readLong(endianness)

fun Reader.readId(): Id =
        when (idSize) {
            IdSize.BITS_8 -> id(readByte())
            IdSize.BITS_16 -> id(readShort())
            IdSize.BITS_32 -> id(readInt())
            IdSize.BITS_64 -> id(readLong())
        }

fun Reader.readByteArray(size: Int): ByteArray =
        inputStream.readByteArray(size)

fun <T> Reader.readList(fn: Reader.() -> T): List<T> =
        PushbackInputStream(inputStream).readList {
            copy(inputStream = this).fn()
        }

fun <T> Reader.readList(size: Int, fn: Reader.() -> T): List<T> =
        inputStream.readList(size) {
            copy(inputStream = this).fn()
        }

fun Reader.readString(): String =
        inputStream.readCString()

fun Reader.readRootSource(): GlobalRoot.Source =
        readByteInt().let { int ->
            when (int) {
                RootSourceTag.GLOBAL -> GlobalRoot.Source.GLOBAL
                RootSourceTag.STABLE_REF -> GlobalRoot.Source.STABLE_REF
                else -> throw IOException("Unknown root source: $int")
            }
        }

fun Reader.readThreadRootSource(): ThreadRoot.Source =
        readByteInt().let { int ->
            when (int) {
                ThreadRootSourceTag.STACK -> ThreadRoot.Source.STACK
                ThreadRootSourceTag.THREAD_LOCAL -> ThreadRoot.Source.THREAD_LOCAL
                else -> throw IOException("Unknown thread root source: $int")
            }
        }

fun Reader.readItem(): Item =
        when (val tag = readByteInt()) {
            RecordTag.TYPE -> {
                val id = readId()
                val flags = readByteInt()
                val isArray = flags.and(0x01) != 0
                val hasExtra = flags.and(0x02) != 0
                val isObjectArray = flags.and(0x04) != 0
                val superTypeId = readId()
                val packageName = readString()
                val relativeName = readString()
                val body =
                        if (isArray) {
                            val elementSize = readInt()
                            val debug = nullUnless(hasExtra) {
                                val elementType = readRuntimeType()
                                Type.Body.Array.Debug(elementType = elementType)
                            }
                            Type.Body.Array(
                                    elementSize = elementSize,
                                    isObjectArray = isObjectArray,
                                    debug = debug
                            )
                        } else {
                            val instanceSize = readInt()
                            val objectOffsets = readList(readInt()) { readInt() }.toIntArray()
                            val debug = nullUnless(hasExtra) {
                                val fields = readList(readInt()) { readField() }
                                Type.Body.Object.Debug(fields = fields)
                            }
                            Type.Body.Object(
                                    instanceSize = instanceSize,
                                    objectOffsets = objectOffsets,
                                    debug = debug,
                            )
                        }
                Type(
                        id,
                        superTypeId,
                        packageName,
                        relativeName,
                        body
                )
            }

            RecordTag.OBJECT -> {
                val id = readId()
                val typeId = readId()
                val size = readInt()
                val byteArray = readByteArray(size)
                ObjectItem(id, typeId, byteArray)
            }

            RecordTag.ARRAY -> {
                val id = readId()
                val typeId = readId()
                val count = readInt()
                val size = readInt()
                val byteArray = readByteArray(size)
                ArrayItem(id, typeId, count, byteArray)
            }

            RecordTag.EXTRA_OBJECT -> {
                val id = readId()
                val baseObjectId = readId()
                val associatedObjectId = readId()
                ExtraObject(id, baseObjectId, associatedObjectId)
            }

            RecordTag.THREAD -> {
                val id = readId()
                Thread(id)
            }

            RecordTag.GLOBAL_ROOT -> {
                val source = readRootSource()
                val objectId = readId()
                GlobalRoot(source, objectId)
            }

            RecordTag.THREAD_ROOT -> {
                val threadId = readId()
                val source = readThreadRootSource()
                val objectId = readId()
                ThreadRoot(threadId, source, objectId)
            }

            else -> throw IOException("Unknown tag: $tag")
        }

fun Reader.readField(): Field = run {
    val offset = readInt()
    val type = readRuntimeType()
    val name = readString()
    Field(offset, type, name)
}

fun Reader.readRuntimeType(): RuntimeType =
        readByteInt().run {
            runtimeTypeOrNull ?: throw IOException("Invalid runtime type: $this")
        }

val Int.runtimeTypeOrNull: RuntimeType?
    get() =
        when (this) {
            1 -> RuntimeType.OBJECT
            2 -> RuntimeType.INT_8
            3 -> RuntimeType.INT_16
            4 -> RuntimeType.INT_32
            5 -> RuntimeType.INT_64
            6 -> RuntimeType.FLOAT_32
            7 -> RuntimeType.FLOAT_64
            8 -> RuntimeType.NATIVE_PTR
            9 -> RuntimeType.BOOLEAN
            10 -> RuntimeType.VECTOR_128
            else -> null
        }
