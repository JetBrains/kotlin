class MessageVarints private constructor (var int: Int, var long: Long, var sint: Int, var slong: Long, var bl: Boolean, var enumField: MessageVarints.TestEnum, var uint: Int, var ulong: Long) {
    //========== Properties ===========
    //int32 int = 1

    //int64 long = 2

    //sint32 sint = 3

    //sint64 slong = 4

    //bool bl = 5

    //enum enumField = 6

    //uint32 uint = 7

    //uint64 ulong = 8

    var errorCode: Int = 0

    //========== Nested enums declarations ===========
    enum class TestEnum(val id: Int) {
        firstVal (0),
        secondVal (1),
        thirdVal (2),
        Unexpected(3);

        companion object {
            fun fromIntToTestEnum (ord: Int): TestEnum {
                return when (ord) {
                    0 -> TestEnum.firstVal
                    1 -> TestEnum.secondVal
                    2 -> TestEnum.thirdVal
                    else -> Unexpected
                }
            }
        }
    }
    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //int32 int = 1
        if (int != 0) {
            output.writeInt32 (1, int)
        }

        //int64 long = 2
        if (long != 0L) {
            output.writeInt64 (2, long)
        }

        //sint32 sint = 3
        if (sint != 0) {
            output.writeSInt32 (3, sint)
        }

        //sint64 slong = 4
        if (slong != 0L) {
            output.writeSInt64 (4, slong)
        }

        //bool bl = 5
        if (bl != false) {
            output.writeBool (5, bl)
        }

        //enum enumField = 6
        if (enumField.id != MessageVarints.TestEnum.fromIntToTestEnum(0).id) {
            output.writeEnum (6, enumField.id)
        }

        //uint32 uint = 7
        if (uint != 0) {
            output.writeUInt32 (7, uint)
        }

        //uint64 ulong = 8
        if (ulong != 0L) {
            output.writeUInt64 (8, ulong)
        }

    }

    fun mergeWith (other: MessageVarints) {
        int = other.int
        long = other.long
        sint = other.sint
        slong = other.slong
        bl = other.bl
        enumField = other.enumField
        uint = other.uint
        ulong = other.ulong
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = MessageVarints.BuilderMessageVarints(0, 0L, 0, 0L, false, MessageVarints.TestEnum.fromIntToTestEnum(0), 0, 0L)
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = MessageVarints.BuilderMessageVarints(0, 0L, 0, 0L, false, MessageVarints.TestEnum.fromIntToTestEnum(0), 0, 0L)
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (int != 0) {
            size += WireFormat.getInt32Size(1, int)
        }
        if (long != 0L) {
            size += WireFormat.getInt64Size(2, long)
        }
        if (sint != 0) {
            size += WireFormat.getSInt32Size(3, sint)
        }
        if (slong != 0L) {
            size += WireFormat.getSInt64Size(4, slong)
        }
        if (bl != false) {
            size += WireFormat.getBoolSize(5, bl)
        }
        if (enumField != MessageVarints.TestEnum.fromIntToTestEnum(0)) {
            size += WireFormat.getEnumSize(6, enumField.id)
        }
        if (uint != 0) {
            size += WireFormat.getUInt32Size(7, uint)
        }
        if (ulong != 0L) {
            size += WireFormat.getUInt64Size(8, ulong)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (int != 0) {
            size += WireFormat.getInt32Size(1, int)
        }
        if (long != 0L) {
            size += WireFormat.getInt64Size(2, long)
        }
        if (sint != 0) {
            size += WireFormat.getSInt32Size(3, sint)
        }
        if (slong != 0L) {
            size += WireFormat.getSInt64Size(4, slong)
        }
        if (bl != false) {
            size += WireFormat.getBoolSize(5, bl)
        }
        if (enumField != MessageVarints.TestEnum.fromIntToTestEnum(0)) {
            size += WireFormat.getEnumSize(6, enumField.id)
        }
        if (uint != 0) {
            size += WireFormat.getUInt32Size(7, uint)
        }
        if (ulong != 0L) {
            size += WireFormat.getUInt64Size(8, ulong)
        }
        return size
    }

    //========== Builder ===========
    class BuilderMessageVarints constructor (var int: Int, var long: Long, var sint: Int, var slong: Long, var bl: Boolean, var enumField: MessageVarints.TestEnum, var uint: Int, var ulong: Long) {
        //========== Properties ===========
        //int32 int = 1
        fun setInt(value: Int): MessageVarints.BuilderMessageVarints {
            int = value
            return this
        }

        //int64 long = 2
        fun setLong(value: Long): MessageVarints.BuilderMessageVarints {
            long = value
            return this
        }

        //sint32 sint = 3
        fun setSint(value: Int): MessageVarints.BuilderMessageVarints {
            sint = value
            return this
        }

        //sint64 slong = 4
        fun setSlong(value: Long): MessageVarints.BuilderMessageVarints {
            slong = value
            return this
        }

        //bool bl = 5
        fun setBl(value: Boolean): MessageVarints.BuilderMessageVarints {
            bl = value
            return this
        }

        //enum enumField = 6
        fun setEnumField(value: MessageVarints.TestEnum): MessageVarints.BuilderMessageVarints {
            enumField = value
            return this
        }

        //uint32 uint = 7
        fun setUint(value: Int): MessageVarints.BuilderMessageVarints {
            uint = value
            return this
        }

        //uint64 ulong = 8
        fun setUlong(value: Long): MessageVarints.BuilderMessageVarints {
            ulong = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //int32 int = 1
            if (int != 0) {
                output.writeInt32 (1, int)
            }

            //int64 long = 2
            if (long != 0L) {
                output.writeInt64 (2, long)
            }

            //sint32 sint = 3
            if (sint != 0) {
                output.writeSInt32 (3, sint)
            }

            //sint64 slong = 4
            if (slong != 0L) {
                output.writeSInt64 (4, slong)
            }

            //bool bl = 5
            if (bl != false) {
                output.writeBool (5, bl)
            }

            //enum enumField = 6
            if (enumField.id != MessageVarints.TestEnum.fromIntToTestEnum(0).id) {
                output.writeEnum (6, enumField.id)
            }

            //uint32 uint = 7
            if (uint != 0) {
                output.writeUInt32 (7, uint)
            }

            //uint64 ulong = 8
            if (ulong != 0L) {
                output.writeUInt64 (8, ulong)
            }

        }

        //========== Mutating methods ===========
        fun build(): MessageVarints {
            val res = MessageVarints(int, long, sint, slong, bl, enumField, uint, ulong)
            res.errorCode = errorCode
            return res
        }

        fun parseFieldFrom(input: CodedInputStream): Boolean {
            if (input.isAtEnd()) { return false }
            val tag = input.readInt32NoTag()
            if (tag == 0) { return false }
            val fieldNumber = WireFormat.getTagFieldNumber(tag)
            val wireType = WireFormat.getTagWireType(tag)
            when(fieldNumber) {
                1 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    int = input.readInt32NoTag()
                }
                2 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    long = input.readInt64NoTag()
                }
                3 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    sint = input.readSInt32NoTag()
                }
                4 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    slong = input.readSInt64NoTag()
                }
                5 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    bl = input.readBoolNoTag()
                }
                6 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    enumField = MessageVarints.TestEnum.fromIntToTestEnum(input.readEnumNoTag())
                }
                7 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    uint = input.readUInt32NoTag()
                }
                8 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    ulong = input.readUInt64NoTag()
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): MessageVarints.BuilderMessageVarints {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): MessageVarints.BuilderMessageVarints {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (int != 0) {
                size += WireFormat.getInt32Size(1, int)
            }
            if (long != 0L) {
                size += WireFormat.getInt64Size(2, long)
            }
            if (sint != 0) {
                size += WireFormat.getSInt32Size(3, sint)
            }
            if (slong != 0L) {
                size += WireFormat.getSInt64Size(4, slong)
            }
            if (bl != false) {
                size += WireFormat.getBoolSize(5, bl)
            }
            if (enumField != MessageVarints.TestEnum.fromIntToTestEnum(0)) {
                size += WireFormat.getEnumSize(6, enumField.id)
            }
            if (uint != 0) {
                size += WireFormat.getUInt32Size(7, uint)
            }
            if (ulong != 0L) {
                size += WireFormat.getUInt64Size(8, ulong)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (int != 0) {
                size += WireFormat.getInt32Size(1, int)
            }
            if (long != 0L) {
                size += WireFormat.getInt64Size(2, long)
            }
            if (sint != 0) {
                size += WireFormat.getSInt32Size(3, sint)
            }
            if (slong != 0L) {
                size += WireFormat.getSInt64Size(4, slong)
            }
            if (bl != false) {
                size += WireFormat.getBoolSize(5, bl)
            }
            if (enumField != MessageVarints.TestEnum.fromIntToTestEnum(0)) {
                size += WireFormat.getEnumSize(6, enumField.id)
            }
            if (uint != 0) {
                size += WireFormat.getUInt32Size(7, uint)
            }
            if (ulong != 0L) {
                size += WireFormat.getUInt64Size(8, ulong)
            }
            return size
        }

    }

}

fun compareVarints(kt1: MessageVarints, kt2: MessageVarints): Boolean {
    return (kt1.int == kt2.int) and
            (kt1.long == kt2.long) and
            (kt1.sint == kt2.sint) and
            (kt1.slong == kt2.slong) and
            (kt1.bl == kt2.bl) and
            (kt1.uint == kt2.uint) and
            (kt1.ulong == kt2.ulong) and
            (kt1.enumField.id == kt2.enumField.id)
}

fun checkVarintsSerializationIdentity(msg: MessageVarints): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = MessageVarints.BuilderMessageVarints(0, 0L, 0, 0L, false, MessageVarints.TestEnum.fromIntToTestEnum(0), 0, 0L)
            .parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareVarints(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

fun testVarintDefaultMessage(): Int {
    val msg = MessageVarints.BuilderMessageVarints(0, 0L, 0, 0L, false, MessageVarints.TestEnum.fromIntToTestEnum(0), 0, 0L).build()
    return checkVarintsSerializationIdentity(msg)
}

fun testVarintTrivialMessage(): Int {
    val msg = MessageVarints.BuilderMessageVarints(
                21312, -2131231231L, 5346734, 42121313211L, true, MessageVarints.TestEnum.fromIntToTestEnum(2), 3672356, 3478787863467834678L
            ).build()
    return checkVarintsSerializationIdentity(msg)
}

fun testVarintNegativeMessage(): Int {
    val msg = MessageVarints.BuilderMessageVarints(
            -312732, -2131231231L, -5346734, -42121313211L, true, MessageVarints.TestEnum.fromIntToTestEnum(3), 3672356, 3478787863467834678L
    ).build()

    return checkVarintsSerializationIdentity(msg)
}

fun testVarintMaxValues(): Int {
    val msg = MessageVarints.BuilderMessageVarints(
            2147483647,
            9223372036854775807L,
            2147483647,
            9223372036854775807L,
            true,
            MessageVarints.TestEnum.fromIntToTestEnum(3),
            2147483647,
            9223372036854775807L
    ).build()

    return checkVarintsSerializationIdentity(msg)
}

fun testVarintMinValues(): Int {
    val msg = MessageVarints.BuilderMessageVarints(
            -2147483647,
            -9223372036854775807L,
            -2147483647,
            -9223372036854775807L,
            false,
            MessageVarints.TestEnum.fromIntToTestEnum(0),
            -2147483647,
            -9223372036854775807L
    ).build()

    return checkVarintsSerializationIdentity(msg)
}