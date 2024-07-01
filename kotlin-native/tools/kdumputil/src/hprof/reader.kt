package hprof

import base.toIntUnsigned
import base.toLongUnsigned
import io.*
import java.io.IOException
import java.io.PushbackInputStream

data class Reader(
        val inputStream: PushbackInputStream,
        val idSize: IdSize,
)

fun Reader.readByte(): Byte =
        inputStream.readByte()

fun Reader.readShort(): Short =
        inputStream.readShort(HPROF_ENDIANNESS)

fun Reader.readInt(): Int =
        inputStream.readInt(HPROF_ENDIANNESS)

fun Reader.readLong(): Long =
        inputStream.readLong(HPROF_ENDIANNESS)

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

fun Reader.readId(): Id =
        Id(
                when (idSize) {
                    IdSize.BYTE -> readByte().toLongUnsigned()
                    IdSize.SHORT -> readShort().toLongUnsigned()
                    IdSize.INT -> readInt().toLongUnsigned()
                    IdSize.LONG -> readLong()
                }
        )

fun Reader.readSerialNumber(): SerialNumber =
        SerialNumber(readInt())

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

fun Reader.readRecord(): Profile.Record {
    val tag = readByte().toIntUnsigned()
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
    val classSerialNumber = readSerialNumber()
    val classObjectId = readId()
    val stackTraceSerialNumber = readSerialNumber()
    val classNameId = readId()
    return LoadClass(classSerialNumber, classObjectId, stackTraceSerialNumber, classNameId)
}

fun Reader.readStackFrame(): StackFrame {
    val stackFrameId = readId()
    val methodNameStringId = readId()
    val methodSignatureStringId = readId()
    val sourceFileNameStringId = readId()
    val classSerialNumber = readSerialNumber()
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
    val stackTraceSerialNumber = readSerialNumber()
    val threadSerialNumber = readSerialNumber()
    val numberOfFrames = readInt()
    val stackFrameIds = inputStream.readList(numberOfFrames) { readId() }
    return StackTrace(stackTraceSerialNumber, threadSerialNumber, stackFrameIds)
}

fun Reader.readStartThread(): StartThread {
    val threadSerialNumber = readSerialNumber()
    val threadObjectId = readId()
    val stackTraceSerialNumber = readSerialNumber()
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
    val tag = readByte().toIntUnsigned()
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
    val threadSerialNumber = readSerialNumber()
    val frameNumber = readInt()
    return RootJniLocal(objectId, threadSerialNumber, frameNumber)
}

fun Reader.readRootStickyClass(): RootStickyClass {
    val objectId = readId()
    return RootStickyClass(objectId)
}

fun Reader.readRootJavaFrame(): RootJavaFrame {
    val objectId = readId()
    val threadSerialNumber = readSerialNumber()
    val frameNumber = readInt()
    return RootJavaFrame(objectId, threadSerialNumber, frameNumber)
}

fun Reader.readRootThreadObject(): RootThreadObject {
    val threadObjectId = readId()
    val threadSerialNumber = readSerialNumber()
    val stackTraceSerialNumber = readSerialNumber()
    return RootThreadObject(threadObjectId, threadSerialNumber, stackTraceSerialNumber)
}

fun Reader.readClassDump(): ClassDump {
    val classObjectId = readId()
    val stackTraceSerialNumber = readSerialNumber()
    val superClassObjectId = readId()
    val classLoaderObjectId = readId()
    val signersObjectId = readId()
    val protectionDomainObjectId = readId()
    val reservedId1 = readId()
    val reservedId2 = readId()
    val instanceSize = readInt()
    val constantsCount = readShort().toIntUnsigned()
    val constants = inputStream.readList(constantsCount) { readConstant() }
    val staticFieldsCount = readShort().toIntUnsigned()
    val staticFields = inputStream.readList(staticFieldsCount) { readStaticField() }
    val instanceFieldCount = readShort().toIntUnsigned()
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
    val stackTraceSerialNumber = readSerialNumber()
    val classObjectId = readId()
    val numberOfBytes = readInt()
    val byteArray = readByteArray(numberOfBytes)
    return InstanceDump(objectId, stackTraceSerialNumber, classObjectId, byteArray)
}

fun Reader.readObjectArrayDump(): ObjectArrayDump {
    val arrayObjectId = readId()
    val stackTraceSerialNumber = readSerialNumber()
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
    val stackTraceSerialNumber = readSerialNumber()
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
