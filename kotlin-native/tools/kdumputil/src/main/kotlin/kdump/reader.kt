package kdump

import base.Endianness
import base.nullUnless
import io.readByte
import io.readByteArray
import io.readByteInt
import io.readCString
import io.readInt
import io.readList
import io.readLong
import io.readShort
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream

fun InputStream.readEndianness(): Endianness {
  val byte = this.readByteInt()
  return if (byte.and(1) != 0) Endianness.LITTLE else Endianness.BIG
}

fun InputStream.readIdSize(): IdSize {
  val size = this.readByteInt()
  return when (size) {
    1 -> IdSize.BITS_8
    2 -> IdSize.BITS_16
    4 -> IdSize.BITS_32
    8 -> IdSize.BITS_64
    else -> throw IOException("Invalid id size: $size.")
  }
}

fun InputStream.readDump(): MemoryDump {
  val headerString = readCString().also {
    if (it != "Kotlin/Native dump 1.0.5") {
      throw IOException("invalid header \"$it\"")
    }
  }
  val endianness = readEndianness()
  val idSize = readIdSize()
  val reader = Reader(this, endianness, idSize)
  val items = reader.readList { readItem() }
  return MemoryDump(
    headerString = headerString,
    endianness = endianness,
    idSize = idSize,
    items = items,
  )
}

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
      val id = readLong()
      val flags = readByteInt()
      val isArray = flags.and(0x01) != 0
      val hasExtra = flags.and(0x02) != 0
      val superTypeId = readLong()
      val packageName = readString()
      val relativeName = readString()
      val body =
        if (isArray) {
          val elementSize = readInt()
          val extra = nullUnless(hasExtra) {
            val elementType = readRuntimeType()
            Type.Body.Array.Extra(elementType = elementType)
          }
          Type.Body.Array(
            elementSize = elementSize,
            extra = extra
          )
        } else {
          val instanceSize = readInt()
          val extra = nullUnless(hasExtra) {
            val fields = readList(readInt()) { readField() }
            Type.Body.Object.Extra(fields = fields)
          }
          Type.Body.Object(
            instanceSize = instanceSize,
            extra = extra
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
      val id = readLong()
      val typeId = readLong()
      val size = readInt()
      val byteArray = readByteArray(size)
      ObjectItem(id, typeId, byteArray)
    }

    RecordTag.ARRAY -> {
      val id = readLong()
      val typeId = readLong()
      val count = readInt()
      val size = readInt()
      val byteArray = readByteArray(size)
      ArrayItem(id, typeId, count, byteArray)
    }

    RecordTag.EXTRA_OBJECT -> {
      val id = readLong()
      val baseObjectId = readLong()
      val associatedObjectId = readLong()
      ExtraObject(id, baseObjectId, associatedObjectId)
    }

    RecordTag.THREAD -> {
      val id = readLong()
      Thread(id)
    }

    RecordTag.GLOBAL_ROOT -> {
      val source = readRootSource()
      val objectId = readLong()
      GlobalRoot(source, objectId)
    }

    RecordTag.THREAD_ROOT -> {
      val threadId = readLong()
      val source = readThreadRootSource()
      val objectId = readLong()
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
