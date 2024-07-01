package hprof

enum class IdSize {
    BYTE,
    SHORT,
    INT,
    LONG,
}

@JvmInline
value class Id(val long: Long) {
    companion object {
        val NULL = Id(0)
    }
}

@JvmInline
value class SerialNumber(val int: Int) {
    companion object {
        val NULL = SerialNumber(0)
    }
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
        val id: Id,
        val string: String,
) : Profile.Record()

data class LoadClass(
        val classSerialNumber: SerialNumber,
        val classObjectId: Id,
        val stackTraceSerialNumber: SerialNumber = SerialNumber.NULL,
        val classNameStringId: Id,
) : Profile.Record()

data class StackFrame(
        val stackFrameId: Id,
        val methodNameStringId: Id,
        val methodSignatureStringId: Id,
        val sourceFileNameStringId: Id = Id.NULL,
        val classSerialNumber: SerialNumber = SerialNumber.NULL,
        val lineNumber: Int = 0,
) : Profile.Record()

data class StackTrace(
        val serialNumber: SerialNumber,
        val threadSerialNumber: SerialNumber,
        val stackFrameIds: List<Id>,
) : Profile.Record()

data class StartThread(
        val threadSerialNumber: SerialNumber,
        val threadObjectId: Id,
        val stackTraceSerialNumber: SerialNumber = SerialNumber.NULL,
        val threadNameStringId: Id = Id.NULL,
        val threadGroupNameId: Id = Id.NULL,
        val threadParentGroupNameId: Id = Id.NULL,
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
        val objectId: Id,
) : HeapDump.Record()

data class RootJniGlobal(
        val objectId: Id,
        val refId: Id,
) : HeapDump.Record()

data class RootJniLocal(
        val objectId: Id,
        val threadSerialNumber: SerialNumber,
        val threadFrameNumber: Int,
) : HeapDump.Record()

data class RootStickyClass(
        val objectId: Id,
) : HeapDump.Record()

data class RootJavaFrame(
        val objectId: Id,
        val threadSerialNumber: SerialNumber,
        val frameNumber: Int = 0,
) : HeapDump.Record()

data class RootThreadObject(
        val threadObjectId: Id,
        val threadSerialNumber: SerialNumber,
        val stackTraceSerialNumber: SerialNumber,
) : HeapDump.Record()

data class ClassDump(
        val classObjectId: Id,
        val stackTraceSerialNumber: SerialNumber = SerialNumber.NULL,
        val superClassObjectId: Id = Id.NULL,
        val classLoaderObjectId: Id = Id.NULL,
        val signersObjectId: Id = Id.NULL,
        val protectionDomainObjectId: Id = Id.NULL,
        val reservedId1: Id = Id.NULL,
        val reservedId2: Id = Id.NULL,
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
        val nameStringId: Id,
        val type: Type,
        val byteArray: ByteArray,
)

data class InstanceField(
        val nameStringId: Id,
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
        val objectId: Id,
        val stackTraceSerialNumber: SerialNumber = SerialNumber.NULL,
        val classObjectId: Id,
        val byteArray: ByteArray = byteArrayOf(),
) : HeapDump.Record()

data class ObjectArrayDump(
        val arrayObjectId: Id,
        val stackTraceSerialNumber: SerialNumber = SerialNumber.NULL,
        val numberOfElements: Int,
        val arrayClassObjectId: Id,
        val byteArray: ByteArray,
) : HeapDump.Record()

data class PrimitiveArrayDump(
        val arrayObjectId: Id,
        val stackTraceSerialNumber: SerialNumber = SerialNumber.NULL,
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
