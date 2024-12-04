package hprof

import io.*
import java.io.*

data class Writer(
        val outputStream: OutputStream,
        val idSize: IdSize,
)

fun Writer.write(byte: Byte) {
    outputStream.writeByte(byte)
}

fun Writer.write(short: Short) {
    outputStream.writeShort(short, HPROF_ENDIANNESS)
}

fun Writer.write(int: Int) {
    outputStream.writeInt(int, HPROF_ENDIANNESS)
}

fun Writer.write(long: Long) {
    outputStream.writeLong(long, HPROF_ENDIANNESS)
}

fun Writer.write(float: Float) {
    outputStream.writeFloat(float, HPROF_ENDIANNESS)
}

fun Writer.write(double: Double) {
    outputStream.writeDouble(double, HPROF_ENDIANNESS)
}

fun Writer.write(byteArray: ByteArray) {
    outputStream.write(byteArray)
}

fun Writer.writeWithSize(byteArray: ByteArray) {
    write(byteArray.size)
    write(byteArray)
}

fun Writer.write(string: String) {
    write(string.toByteArray(Charsets.UTF_8))
}

fun <T> Writer.write(list: List<T>, fn: Writer.(T) -> Unit) {
    list.forEach { fn(it) }
}

fun <T> Writer.writeWithShortSize(list: List<T>, fn: Writer.(T) -> Unit) {
    write(list.size.toShort())
    list.forEach { fn(it) }
}

fun Writer.write(id: Id) {
    when (idSize) {
        IdSize.BYTE -> write(id.long.toByte())
        IdSize.SHORT -> write(id.long.toShort())
        IdSize.INT -> write(id.long.toInt())
        IdSize.LONG -> write(id.long)
    }
}

fun Writer.write(serialNumber: SerialNumber) {
    write(serialNumber.int)
}

fun Writer.write(type: Type) {
    write(
            when (type) {
                Type.OBJECT -> Binary.Type.OBJECT
                Type.BOOLEAN -> Binary.Type.BOOLEAN
                Type.CHAR -> Binary.Type.CHAR
                Type.BYTE -> Binary.Type.BYTE
                Type.SHORT -> Binary.Type.SHORT
                Type.INT -> Binary.Type.INT
                Type.LONG -> Binary.Type.LONG
                Type.FLOAT -> Binary.Type.FLOAT
                Type.DOUBLE -> Binary.Type.DOUBLE
            }.toByte()
    )
}

fun Writer.writeProfileRecord(tag: Int, fn: Writer.() -> Unit) {
    write(tag.toByte())
    write(0) // time
    val byteArray = ByteArrayOutputStream()
            .apply { Writer(this, idSize).fn() }
            .toByteArray()
    write(byteArray.size)
    write(byteArray)
}

fun Writer.write(it: Profile.Record) {
    when (it) {
        is UnknownRecord -> writeProfileRecord(it.tag) { writeBody(it) }
        is StringConstant -> writeProfileRecord(Binary.Profile.Record.Tag.STRING_CONSTANT) {
            writeBody(
                    it
            )
        }

        is LoadClass -> writeProfileRecord(Binary.Profile.Record.Tag.LOAD_CLASS) { writeBody(it) }
        is StackFrame -> writeProfileRecord(Binary.Profile.Record.Tag.STACK_FRAME) { writeBody(it) }
        is StackTrace -> writeProfileRecord(Binary.Profile.Record.Tag.STACK_TRACE) { writeBody(it) }
        is StartThread -> writeProfileRecord(Binary.Profile.Record.Tag.START_THREAD) { writeBody(it) }
        is HeapDump -> writeProfileRecord(Binary.Profile.Record.Tag.HEAP_DUMP) { writeBody(it) }
        is HeapDumpSection -> writeProfileRecord(Binary.Profile.Record.Tag.HEAP_DUMP_SECTION) {
            writeBody(
                    it
            )
        }

        is HeapDumpEnd -> writeProfileRecord(Binary.Profile.Record.Tag.HEAP_DUMP_END) { writeBody(it) }
    }
}

fun Writer.writeBody(it: UnknownRecord) {
    write(it.byteArray)
}

fun Writer.writeBody(it: StringConstant) {
    write(it.id)
    write(it.string.toByteArray(Charsets.UTF_8))
}

fun Writer.writeBody(it: LoadClass) {
    write(it.classSerialNumber)
    write(it.classObjectId)
    write(it.stackTraceSerialNumber)
    write(it.classNameStringId)
}

fun Writer.writeBody(it: StackFrame) {
    write(it.stackFrameId)
    write(it.methodNameStringId)
    write(it.methodSignatureStringId)
    write(it.sourceFileNameStringId)
    write(it.classSerialNumber)
    write(it.lineNumber)
}

fun Writer.writeBody(it: StackTrace) {
    write(it.serialNumber)
    write(it.threadSerialNumber)
    write(it.stackFrameIds.size)
    it.stackFrameIds.forEach { write(it) }
}

fun Writer.writeBody(it: StartThread) {
    write(it.threadSerialNumber)
    write(it.threadObjectId)
    write(it.stackTraceSerialNumber)
    write(it.threadNameStringId)
    write(it.threadGroupNameId)
    write(it.threadParentGroupNameId)
}

fun Writer.writeBody(it: HeapDump) {
    write(it.records) { write(it) }
}

fun Writer.writeBody(it: HeapDumpSection) {
    write(it.records) { write(it) }
}

fun Writer.writeBody(@Suppress("UNUSED_PARAMETER") it: HeapDumpEnd) {

}

fun Writer.write(it: HeapDump.Record) {
    when (it) {
        is RootUnknown -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.ROOT_UNKNOWN) { writeBody(it) }
        is RootJniGlobal -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.ROOT_JNI_GLOBAL) {
            writeBody(
                    it
            )
        }

        is RootJniLocal -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.ROOT_JNI_LOCAL) { writeBody(it) }
        is RootJavaFrame -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.ROOT_JAVA_FRAME) {
            writeBody(
                    it
            )
        }

        is RootStickyClass -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.ROOT_STICKY_CLASS) {
            writeBody(
                    it
            )
        }

        is RootThreadObject -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.ROOT_THREAD_OBJECT) {
            writeBody(
                    it
            )
        }

        is ClassDump -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.CLASS_DUMP) { writeBody(it) }
        is InstanceDump -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.INSTANCE_DUMP) { writeBody(it) }
        is ObjectArrayDump -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.OBJECT_ARRAY_DUMP) {
            writeBody(
                    it
            )
        }

        is PrimitiveArrayDump -> writeHeapDumpRecord(Binary.HeapDump.Record.Tag.PRIMITIVE_ARRAY_DUMP) {
            writeBody(
                    it
            )
        }
    }
}

fun Writer.writeHeapDumpRecord(tag: Int, fn: Writer.() -> Unit) {
    write(tag.toByte())
    fn()
}

fun Writer.writeBody(it: RootUnknown) {
    write(it.objectId)
}

fun Writer.writeBody(it: RootJniGlobal) {
    write(it.objectId)
    write(it.refId)
}

fun Writer.writeBody(it: RootJniLocal) {
    write(it.objectId)
    write(it.threadSerialNumber)
    write(it.threadFrameNumber)
}

fun Writer.writeBody(it: RootStickyClass) {
    write(it.objectId)
}

fun Writer.writeBody(it: RootJavaFrame) {
    write(it.objectId)
    write(it.threadSerialNumber)
    write(it.frameNumber)
}

fun Writer.writeBody(it: RootThreadObject) {
    write(it.threadObjectId)
    write(it.threadSerialNumber)
    write(it.stackTraceSerialNumber)
}

fun Writer.writeBody(it: ClassDump) {
    write(it.classObjectId)
    write(it.stackTraceSerialNumber)
    write(it.superClassObjectId)
    write(it.classLoaderObjectId)
    write(it.signersObjectId)
    write(it.protectionDomainObjectId)
    write(it.reservedId1)
    write(it.reservedId2)
    write(it.instanceSize)
    writeWithShortSize(it.constants) { write(it) }
    writeWithShortSize(it.staticFields) { write(it) }
    writeWithShortSize(it.instanceFields) { write(it) }
}

fun Writer.write(it: Constant) {
    write(it.index)
    write(it.type)
    write(it.byteArray)
}

fun Writer.write(it: StaticField) {
    write(it.nameStringId)
    write(it.type)
    write(it.byteArray)
}

fun Writer.write(it: InstanceField) {
    write(it.nameStringId)
    write(it.type)
}

fun Writer.writeBody(it: InstanceDump) {
    write(it.objectId)
    write(it.stackTraceSerialNumber)
    write(it.classObjectId)
    writeWithSize(it.byteArray)
}

fun Writer.writeBody(it: ObjectArrayDump) {
    write(it.arrayObjectId)
    write(it.stackTraceSerialNumber)
    write(it.numberOfElements)
    write(it.arrayClassObjectId)
    write(it.byteArray)
}

fun Writer.writeBody(it: PrimitiveArrayDump) {
    write(it.arrayObjectId)
    write(it.stackTraceSerialNumber)
    write(it.numberOfElements)
    write(it.arrayElementType)
    write(it.byteArray)
}
