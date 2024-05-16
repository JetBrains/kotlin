package hprof

enum class IdSize {
  BYTE,
  SHORT,
  INT,
  LONG,
}

data class Profile(
  val idSize: IdSize,
  val time: Long,
  val records: List<Record>,
) {
  sealed class Record
}

data class UnknownRecord(
  val tag: Int,
  val byteArray: ByteArray,
) : Profile.Record()

data class StringConstant(
  val id: Long,
  val string: String,
) : Profile.Record()

data class LoadClass(
  val classSerialNumber: Int,
  val classObjectId: Long,
  val stackTraceSerialNumber: Int = 0,
  val classNameStringId: Long,
) : Profile.Record()

data class StackFrame(
  val stackFrameId: Long,
  val methodNameStringId: Long,
  val methodSignatureStringId: Long,
  val sourceFileNameStringId: Long = 0,
  val classSerialNumber: Int = 0,
  val lineNumber: Int = 0,
) : Profile.Record()

data class StackTrace(
  val serialNumber: Int,
  val threadSerialNumber: Int,
  val stackFrameIds: LongArray,
) : Profile.Record()

data class StartThread(
  val threadSerialNumber: Int,
  val threadObjectId: Long,
  val stackTraceSerialNumber: Int = 0,
  val threadNameStringId: Long = 0,
  val threadGroupNameId: Long = 0,
  val threadParentGroupNameId: Long = 0,
) : Profile.Record()

data class HeapDump(
  val records: List<Record>,
) : Profile.Record() {
  sealed class Record
}

data class HeapDumpSection(
  val records: List<HeapDump.Record>,
) : Profile.Record()

data object HeapDumpEnd : Profile.Record()

data class RootUnknown(
  val objectId: Long,
) : HeapDump.Record()

data class RootJniGlobal(
  val objectId: Long,
  val refId: Long,
) : HeapDump.Record()

data class RootJniLocal(
  val objectId: Long,
  val threadSerialNumber: Int,
  val threadFrameNumber: Int,
) : HeapDump.Record()

data class RootStickyClass(
  val objectId: Long,
) : HeapDump.Record()

data class RootJavaFrame(
  val objectId: Long,
  val threadSerialNumber: Int,
  val frameNumber: Int = 0,
) : HeapDump.Record()

data class RootThreadObject(
  val threadObjectId: Long,
  val threadSerialNumber: Int,
  val stackTraceSerialNumber: Int,
) : HeapDump.Record()

data class ClassDump(
  val classObjectId: Long,
  val stackTraceSerialNumber: Int = 0,
  val superClassObjectId: Long = 0L,
  val classLoaderObjectId: Long = 0L,
  val signersObjectId: Long = 0L,
  val protectionDomainObjectId: Long = 0L,
  val reservedId1: Long = 0L,
  val reservedId2: Long = 0L,
  val instanceSize: Int = 0,
  val constants: List<Constant> = listOf(),
  val staticFields: List<StaticField> = listOf(),
  val instanceFields: List<InstanceField> = listOf(),
) : HeapDump.Record()

data class Constant(
  val index: Short,
  val type: Type,
  val byteArray: ByteArray,
)

data class StaticField(
  val nameStringId: Long,
  val type: Type,
  val byteArray: ByteArray,
)

data class InstanceField(
  val nameStringId: Long,
  val type: Type,
)

enum class Type {
  OBJECT,
  BOOLEAN,
  CHAR,
  BYTE,
  SHORT,
  INT,
  LONG,
  FLOAT,
  DOUBLE,
}

data class InstanceDump(
  val objectId: Long,
  val stackTraceSerialNumber: Int = 0,
  val classObjectId: Long,
  val byteArray: ByteArray = byteArrayOf(),
) : HeapDump.Record()

data class ObjectArrayDump(
  val arrayObjectId: Long,
  val stackTraceSerialNumber: Int = 0,
  val numberOfElements: Int,
  val arrayClassObjectId: Long,
  val byteArray: ByteArray,
) : HeapDump.Record()

data class PrimitiveArrayDump(
  val arrayObjectId: Long,
  val stackTraceSerialNumber: Int = 0,
  val numberOfElements: Int,
  val arrayElementType: Type,
  val byteArray: ByteArray,
) : HeapDump.Record()

val IdSize.size: Int
  get() =
    when (this) {
      IdSize.BYTE -> 1
      IdSize.SHORT -> 2
      IdSize.INT -> 4
      IdSize.LONG -> 8
    }

fun Type.size(idSize: IdSize): Int =
  when (this) {
    Type.OBJECT -> idSize.size
    Type.BOOLEAN -> 1
    Type.CHAR -> 2
    Type.BYTE -> 1
    Type.SHORT -> 2
    Type.INT -> 4
    Type.LONG -> 8
    Type.FLOAT -> 4
    Type.DOUBLE -> 8
  }

val Type.primitiveSize: Int
  get() =
    when (this) {
      Type.OBJECT -> throw IllegalArgumentException()
      Type.BOOLEAN -> 1
      Type.CHAR -> 2
      Type.BYTE -> 1
      Type.SHORT -> 2
      Type.INT -> 4
      Type.LONG -> 8
      Type.FLOAT -> 4
      Type.DOUBLE -> 8
    }
