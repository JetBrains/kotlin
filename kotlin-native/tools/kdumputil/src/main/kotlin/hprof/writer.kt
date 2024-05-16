package hprof

import base.Endianness
import io.*
import java.io.*

fun OutputStream.write(idSize: IdSize) {
  writeInt(idSize.size, Endianness.BIG)
}

fun OutputStream.write(it: Profile) {
  write("JAVA PROFILE 1.0.2")
  write(it.idSize)
  writeLong(it.time, Endianness.BIG)
  HProfWriter(this, it.idSize).run {
    write(it.records) { write(it) }
  }
}

data class HProfWriter(
  val outputStream: OutputStream,
  val idSize: IdSize,
)

fun HProfWriter.write(byte: Byte) {
  outputStream.writeByte(byte)
}

fun HProfWriter.write(short: Short) {
  outputStream.writeShort(short, Endianness.BIG)
}

fun HProfWriter.write(int: Int) {
  outputStream.writeInt(int, Endianness.BIG)
}

fun HProfWriter.write(long: Long) {
  outputStream.writeLong(long, Endianness.BIG)
}

fun HProfWriter.write(byteArray: ByteArray) {
  outputStream.write(byteArray)
}

fun HProfWriter.writeWithSize(byteArray: ByteArray) {
  write(byteArray.size)
  write(byteArray)
}

fun HProfWriter.write(string: String) {
  write(string.toByteArray(Charsets.UTF_8))
}

fun <T> HProfWriter.write(list: List<T>, fn: HProfWriter.(T) -> Unit) {
  list.forEach { fn(it) }
}

fun <T> HProfWriter.writeWithShortSize(list: List<T>, fn: HProfWriter.(T) -> Unit) {
  write(list.size.toShort())
  list.forEach { fn(it) }
}

fun HProfWriter.writeId(id: Long) {
  when (idSize) {
    IdSize.BYTE -> write(id.toByte())
    IdSize.SHORT -> write(id.toShort())
    IdSize.INT -> write(id.toInt())
    IdSize.LONG -> write(id)
  }
}

fun HProfWriter.write(type: Type) {
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

fun HProfWriter.writeProfileRecord(tag: Int, fn: HProfWriter.() -> Unit) {
  write(tag.toByte())
  write(0) // time
  val byteArray = ByteArrayOutputStream()
    .apply { HProfWriter(this, idSize).fn() }
    .toByteArray()
  write(byteArray.size)
  write(byteArray)
}

fun HProfWriter.write(it: Profile.Record) {
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

fun HProfWriter.writeBody(it: UnknownRecord) {
  write(it.byteArray)
}

fun HProfWriter.writeBody(it: StringConstant) {
  writeId(it.id)
  write(it.string.toByteArray(Charsets.UTF_8))
}

fun HProfWriter.writeBody(it: LoadClass) {
  write(it.classSerialNumber)
  writeId(it.classObjectId)
  write(it.stackTraceSerialNumber)
  writeId(it.classNameStringId)
}

fun HProfWriter.writeBody(it: StackFrame) {
  writeId(it.stackFrameId)
  writeId(it.methodNameStringId)
  writeId(it.methodSignatureStringId)
  writeId(it.sourceFileNameStringId)
  write(it.classSerialNumber)
  write(it.lineNumber)
}

fun HProfWriter.writeBody(it: StackTrace) {
  write(it.serialNumber)
  write(it.threadSerialNumber)
  write(it.stackFrameIds.size)
  it.stackFrameIds.forEach { write(it) }
}

fun HProfWriter.writeBody(it: StartThread) {
  write(it.threadSerialNumber)
  write(it.threadObjectId)
  write(it.stackTraceSerialNumber)
  write(it.threadNameStringId)
  write(it.threadGroupNameId)
  write(it.threadParentGroupNameId)
}

fun HProfWriter.writeBody(it: HeapDump) {
  write(it.records) { write(it) }
}

fun HProfWriter.writeBody(it: HeapDumpSection) {
  write(it.records) { write(it) }
}

fun HProfWriter.writeBody(@Suppress("UNUSED_PARAMETER") it: HeapDumpEnd) {

}

fun HProfWriter.write(it: HeapDump.Record) {
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

fun HProfWriter.writeHeapDumpRecord(tag: Int, fn: HProfWriter.() -> Unit) {
  write(tag.toByte())
  fn()
}

fun HProfWriter.writeBody(it: RootUnknown) {
  writeId(it.objectId)
}

fun HProfWriter.writeBody(it: RootJniGlobal) {
  writeId(it.objectId)
  writeId(it.refId)
}

fun HProfWriter.writeBody(it: RootJniLocal) {
  writeId(it.objectId)
  write(it.threadSerialNumber)
  write(it.threadFrameNumber)
}

fun HProfWriter.writeBody(it: RootStickyClass) {
  writeId(it.objectId)
}

fun HProfWriter.writeBody(it: RootJavaFrame) {
  writeId(it.objectId)
  write(it.threadSerialNumber)
  write(it.frameNumber)
}

fun HProfWriter.writeBody(it: RootThreadObject) {
  writeId(it.threadObjectId)
  write(it.threadSerialNumber)
  write(it.stackTraceSerialNumber)
}

fun HProfWriter.writeBody(it: ClassDump) {
  writeId(it.classObjectId)
  write(it.stackTraceSerialNumber)
  writeId(it.superClassObjectId)
  writeId(it.classLoaderObjectId)
  writeId(it.signersObjectId)
  writeId(it.protectionDomainObjectId)
  writeId(it.reservedId1)
  writeId(it.reservedId2)
  write(it.instanceSize)
  writeWithShortSize(it.constants) { write(it) }
  writeWithShortSize(it.staticFields) { write(it) }
  writeWithShortSize(it.instanceFields) { write(it) }
}

fun HProfWriter.write(it: Constant) {
  write(it.index)
  write(it.type)
  write(it.byteArray)
}

fun HProfWriter.write(it: StaticField) {
  writeId(it.nameStringId)
  write(it.type)
  write(it.byteArray)
}

fun HProfWriter.write(it: InstanceField) {
  writeId(it.nameStringId)
  write(it.type)
}

fun HProfWriter.writeBody(it: InstanceDump) {
  writeId(it.objectId)
  write(it.stackTraceSerialNumber)
  writeId(it.classObjectId)
  writeWithSize(it.byteArray)
}

fun HProfWriter.writeBody(it: ObjectArrayDump) {
  writeId(it.arrayObjectId)
  write(it.stackTraceSerialNumber)
  write(it.numberOfElements)
  writeId(it.arrayClassObjectId)
  write(it.byteArray)
}

fun HProfWriter.writeBody(it: PrimitiveArrayDump) {
  writeId(it.arrayObjectId)
  write(it.stackTraceSerialNumber)
  write(it.numberOfElements)
  write(it.arrayElementType)
  write(it.byteArray)
}
