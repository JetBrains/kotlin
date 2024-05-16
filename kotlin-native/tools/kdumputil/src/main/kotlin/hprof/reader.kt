package hprof

import base.Endianness
import io.readByte
import io.readByteArray
import io.readCString
import io.readInt
import io.readList
import io.readLong
import io.readShort
import io.readWithSize
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream

data class Reader(
  val inputStream: PushbackInputStream,
  val idSize: IdSize,
)

fun Reader.readByte(): Byte =
  inputStream.readByte()

fun Reader.readShort(): Short =
  inputStream.readShort(Endianness.BIG)

fun Reader.readInt(): Int =
  inputStream.readInt(Endianness.BIG)

fun Reader.readLong(): Long =
  inputStream.readLong(Endianness.BIG)

fun Reader.readByteArray(size: Int): ByteArray =
  inputStream.readByteArray(size)

fun Reader.readByteArray(): ByteArray =
  inputStream.readByteArray()

fun <T> Reader.readList(size: Int, fn: Reader.() -> T): List<T> =
  inputStream.readList(size) {
    Reader(inputStream, idSize).fn()
  }

fun <T> Reader.readList(fn: Reader.() -> T): List<T> =
  inputStream.readList {
    Reader(PushbackInputStream(this), idSize).fn()
  }

fun InputStream.readIdSize(): IdSize = run {
  when (val idSizeInt = readInt(Endianness.BIG)) {
    1 -> IdSize.BYTE
    2 -> IdSize.SHORT
    4 -> IdSize.INT
    8 -> IdSize.LONG
    else -> throw IOException("Unknown ID size: $idSizeInt")
  }
}

fun Reader.readId(): Long =
  when (idSize) {
    IdSize.BYTE -> readByte().toInt().and(0xff).toLong()
    IdSize.SHORT -> readShort().toInt().and(0xffff).toLong()
    IdSize.INT -> readInt().toLong().and(0xffffffffL)
    IdSize.LONG -> readLong()
  }

fun Reader.readType(): Type {
  val int = readByte().toInt().and(0xFF)
  return when (int) {
    Binary.Type.OBJECT -> Type.OBJECT
    Binary.Type.BOOLEAN -> Type.BOOLEAN
    Binary.Type.CHAR -> Type.CHAR
    Binary.Type.FLOAT -> Type.FLOAT
    Binary.Type.DOUBLE -> Type.DOUBLE
    Binary.Type.BYTE -> Type.BYTE
    Binary.Type.SHORT -> Type.SHORT
    Binary.Type.INT -> Type.INT
    Binary.Type.LONG -> Type.LONG
    else -> throw IOException("Unknown type: $int")
  }
}

fun <T> Reader.readWithSize(size: Int, fn: Reader.() -> T): T =
  inputStream.readWithSize(size) {
    Reader(PushbackInputStream(this), idSize).fn()
  }

fun InputStream.readProfile(): Profile {
  readCString().also {
    if (it != "JAVA PROFILE 1.0.2") {
      throw IOException("invalid header \"$it\"")
    }
  }
  val idSize = readIdSize()
  val time = readLong(Endianness.BIG)
  val records = Reader(PushbackInputStream(this), idSize).readList { readRecord() }
  return Profile(idSize, time, records)
}

fun Reader.readRecord(): Profile.Record {
  val tag = readByte().toInt().and(0xff)
  /* val time = */ readInt()
  val length = readInt()
  return readWithSize(length) { readRecord(tag) }
}

fun Reader.readRecord(tag: Int): Profile.Record =
  when (tag) {
    Binary.Profile.Record.Tag.STRING_CONSTANT -> readStringConstant()
    Binary.Profile.Record.Tag.LOAD_CLASS -> readLoadClass()
    Binary.Profile.Record.Tag.STACK_FRAME -> readStackFrame()
    Binary.Profile.Record.Tag.STACK_TRACE -> readStackTrace()
    Binary.Profile.Record.Tag.START_THREAD -> readStartThread()
    Binary.Profile.Record.Tag.HEAP_DUMP -> readHeapDump()
    Binary.Profile.Record.Tag.HEAP_DUMP_SECTION -> readHeapDumpSection()
    Binary.Profile.Record.Tag.HEAP_DUMP_END -> readHeapDumpEnd()
    else -> readUnknownRecord(tag)
  }

fun Reader.readUnknownRecord(tag: Int): UnknownRecord {
  val byteArray = readByteArray()
  return UnknownRecord(tag, byteArray)
}

fun Reader.readStringConstant(): StringConstant {
  val id = readId()
  val byteArray = readByteArray()
  return StringConstant(id, byteArray.toString(Charsets.UTF_8))
}

fun Reader.readLoadClass(): LoadClass {
  val classSerialNumber = readInt()
  val classObjectId = readId()
  val stackTraceSerialNumber = readInt()
  val classNameId = readId()
  return LoadClass(classSerialNumber, classObjectId, stackTraceSerialNumber, classNameId)
}

fun Reader.readStackFrame(): StackFrame {
  val stackFrameId = readId()
  val methodNameStringId = readId()
  val methodSignatureStringId = readId()
  val sourceFileNameStringId = readId()
  val classSerialNumber = readInt()
  val lineNumber = readInt()
  return StackFrame(
    stackFrameId,
    methodNameStringId,
    methodSignatureStringId,
    sourceFileNameStringId,
    classSerialNumber,
    lineNumber
  )
}

fun Reader.readStackTrace(): StackTrace {
  val stackTraceSerialNumber = readInt()
  val threadSerialNumber = readInt()
  val numberOfFrames = readInt()
  val stackFrameIds = inputStream.readList(numberOfFrames) { readId() }
  return StackTrace(stackTraceSerialNumber, threadSerialNumber, stackFrameIds.toLongArray())
}

fun Reader.readStartThread(): StartThread {
  val threadSerialNumber = readInt()
  val threadObjectId = readId()
  val stackTraceSerialNumber = readInt()
  val threadNameStringId = readId()
  val threadGroupNameId = readId()
  val threadParentGroupNameId = readId()
  return StartThread(
    threadSerialNumber,
    threadObjectId,
    stackTraceSerialNumber,
    threadNameStringId,
    threadGroupNameId,
    threadParentGroupNameId
  )
}

fun Reader.readHeapDump(): HeapDump {
  val records = readList { readHeapDumpRecord() }
  return HeapDump(records)
}

fun Reader.readHeapDumpSection(): HeapDumpSection {
  val records = readList { readHeapDumpRecord() }
  return HeapDumpSection(records)
}

fun Reader.readHeapDumpEnd(): HeapDumpEnd {
  return HeapDumpEnd
}

fun Reader.readHeapDumpRecord(): HeapDump.Record {
  val tag = readByte().toInt().and(0xff)
  return when (tag) {
    Binary.HeapDump.Record.Tag.ROOT_JNI_GLOBAL -> readRootJniGlobal()
    Binary.HeapDump.Record.Tag.ROOT_JNI_LOCAL -> readRootJniLocal()
    Binary.HeapDump.Record.Tag.ROOT_JAVA_FRAME -> readRootJavaFrame()
    Binary.HeapDump.Record.Tag.ROOT_STICKY_CLASS -> readRootStickyClass()
    Binary.HeapDump.Record.Tag.ROOT_THREAD_OBJECT -> readRootThreadObject()
    Binary.HeapDump.Record.Tag.CLASS_DUMP -> readClassDump()
    Binary.HeapDump.Record.Tag.INSTANCE_DUMP -> readInstanceDump()
    Binary.HeapDump.Record.Tag.OBJECT_ARRAY_DUMP -> readObjectArrayDump()
    Binary.HeapDump.Record.Tag.PRIMITIVE_ARRAY_DUMP -> readPrimitiveArrayDump()
    else -> throw IOException("Unknown heap dump tag: $tag")
  }
}

fun Reader.readRootJniGlobal(): RootJniGlobal {
  val objectId = readId()
  val jniGlobalRefId = readId()
  return RootJniGlobal(objectId, jniGlobalRefId)
}

fun Reader.readRootJniLocal(): RootJniLocal {
  val objectId = readId()
  val threadSerialNumber = readInt()
  val frameNumber = readInt()
  return RootJniLocal(objectId, threadSerialNumber, frameNumber)
}

fun Reader.readRootStickyClass(): RootStickyClass {
  val objectId = readId()
  return RootStickyClass(objectId)
}

fun Reader.readRootJavaFrame(): RootJavaFrame {
  val objectId = readId()
  val threadSerialNumber = readInt()
  val frameNumber = readInt()
  return RootJavaFrame(objectId, threadSerialNumber, frameNumber)
}

fun Reader.readRootThreadObject(): RootThreadObject {
  val threadObjectId = readId()
  val threadSerialNumber = readInt()
  val stackTraceSerialNumber = readInt()
  return RootThreadObject(threadObjectId, threadSerialNumber, stackTraceSerialNumber)
}

fun Reader.readClassDump(): ClassDump {
  val classObjectId = readId()
  val stackTraceSerialNumber = readInt()
  val superClassObjectId = readId()
  val classLoaderObjectId = readId()
  val signersObjectId = readId()
  val protectionDomainObjectId = readId()
  val reservedId1 = readId()
  val reservedId2 = readId()
  val instanceSize = readInt()
  val constantsCount = readShort().toInt().and(0xffff)
  val constants = inputStream.readList(constantsCount) { readConstant() }
  val staticFieldsCount = readShort().toInt().and(0xffff)
  val staticFields = inputStream.readList(staticFieldsCount) { readStaticField() }
  val instanceFieldCount = readShort().toInt().and(0xffff)
  val instanceFields = inputStream.readList(instanceFieldCount) { readInstanceField() }
  return ClassDump(
    classObjectId,
    stackTraceSerialNumber,
    superClassObjectId,
    classLoaderObjectId,
    signersObjectId,
    protectionDomainObjectId,
    reservedId1,
    reservedId2,
    instanceSize,
    constants,
    staticFields,
    instanceFields
  )
}

fun Reader.readConstant(): Constant {
  val index = readShort()
  val type = readType()
  val byteArray = readByteArray(type.size(idSize))
  return Constant(index, type, byteArray)
}

fun Reader.readStaticField(): StaticField {
  val nameStringId = readId()
  val type = readType()
  val byteArray = readByteArray(type.size(idSize))
  return StaticField(nameStringId, type, byteArray)
}

fun Reader.readInstanceField(): InstanceField {
  val nameStringId = readId()
  val type = readType()
  return InstanceField(nameStringId, type)
}

fun Reader.readInstanceDump(): InstanceDump {
  val objectId = readId()
  val stackTraceSerialNumber = readInt()
  val classObjectId = readId()
  val numberOfBytes = readInt()
  val byteArray = readByteArray(numberOfBytes)
  return InstanceDump(objectId, stackTraceSerialNumber, classObjectId, byteArray)
}

fun Reader.readObjectArrayDump(): ObjectArrayDump {
  val arrayObjectId = readId()
  val stackTraceSerialNumber = readInt()
  val numberOfElement = readInt()
  val arrayClassObjectId = readId()
  val byteArray = readByteArray(Type.OBJECT.size(idSize) * numberOfElement)
  return ObjectArrayDump(
    arrayObjectId,
    stackTraceSerialNumber,
    numberOfElement,
    arrayClassObjectId,
    byteArray
  )
}

fun Reader.readPrimitiveArrayDump(): PrimitiveArrayDump {
  val arrayObjectId = readId()
  val stackTraceSerialNumber = readInt()
  val numberOfElement = readInt()
  val arrayElementType = readType()
  val byteArray = readByteArray(arrayElementType.size(idSize) * numberOfElement)
  return PrimitiveArrayDump(
    arrayObjectId,
    stackTraceSerialNumber,
    numberOfElement,
    arrayElementType,
    byteArray
  )
}
