class simple_test private constructor (var field1: Int, var field2: Long, var field3: Int, var field4: Long, var field5: Boolean) {
    //========== Properties ===========
    //int32 field1 = 1

    //int64 field2 = 2

    //sint32 field3 = 3

    //sint64 field4 = 4

    //bool field5 = 5

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //int32 field1 = 1
        if (field1 != 0) {
            output.writeInt32 (1, field1)
        }

        //int64 field2 = 2
        if (field2 != 0L) {
            output.writeInt64 (2, field2)
        }

        //sint32 field3 = 3
        if (field3 != 0) {
            output.writeSInt32 (3, field3)
        }

        //sint64 field4 = 4
        if (field4 != 0L) {
            output.writeSInt64 (4, field4)
        }

        //bool field5 = 5
        if (field5 != false) {
            output.writeBool (5, field5)
        }

    }

    fun mergeWith (other: simple_test) {
        field1 = other.field1
        field2 = other.field2
        field3 = other.field3
        field4 = other.field4
        field5 = other.field5
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = simple_test.Buildersimple_test(0, 0L, 0, 0L, false)
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = simple_test.Buildersimple_test(0, 0L, 0, 0L, false)
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (field1 != 0) {
            size += WireFormat.getInt32Size(1, field1)
        }
        if (field2 != 0L) {
            size += WireFormat.getInt64Size(2, field2)
        }
        if (field3 != 0) {
            size += WireFormat.getSInt32Size(3, field3)
        }
        if (field4 != 0L) {
            size += WireFormat.getSInt64Size(4, field4)
        }
        if (field5 != false) {
            size += WireFormat.getBoolSize(5, field5)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (field1 != 0) {
            size += WireFormat.getInt32Size(1, field1)
        }
        if (field2 != 0L) {
            size += WireFormat.getInt64Size(2, field2)
        }
        if (field3 != 0) {
            size += WireFormat.getSInt32Size(3, field3)
        }
        if (field4 != 0L) {
            size += WireFormat.getSInt64Size(4, field4)
        }
        if (field5 != false) {
            size += WireFormat.getBoolSize(5, field5)
        }
        return size
    }

    //========== Builder ===========
    class Buildersimple_test constructor (var field1: Int, var field2: Long, var field3: Int, var field4: Long, var field5: Boolean) {
        //========== Properties ===========
        //int32 field1 = 1
        fun setField1(value: Int): simple_test.Buildersimple_test {
            field1 = value
            return this
        }

        //int64 field2 = 2
        fun setField2(value: Long): simple_test.Buildersimple_test {
            field2 = value
            return this
        }

        //sint32 field3 = 3
        fun setField3(value: Int): simple_test.Buildersimple_test {
            field3 = value
            return this
        }

        //sint64 field4 = 4
        fun setField4(value: Long): simple_test.Buildersimple_test {
            field4 = value
            return this
        }

        //bool field5 = 5
        fun setField5(value: Boolean): simple_test.Buildersimple_test {
            field5 = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //int32 field1 = 1
            if (field1 != 0) {
                output.writeInt32 (1, field1)
            }

            //int64 field2 = 2
            if (field2 != 0L) {
                output.writeInt64 (2, field2)
            }

            //sint32 field3 = 3
            if (field3 != 0) {
                output.writeSInt32 (3, field3)
            }

            //sint64 field4 = 4
            if (field4 != 0L) {
                output.writeSInt64 (4, field4)
            }

            //bool field5 = 5
            if (field5 != false) {
                output.writeBool (5, field5)
            }

        }

        //========== Mutating methods ===========
        fun build(): simple_test {
            val res = simple_test(field1, field2, field3, field4, field5)
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
                    field1 = input.readInt32NoTag()
                }
                2 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    field2 = input.readInt64NoTag()
                }
                3 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    field3 = input.readSInt32NoTag()
                }
                4 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    field4 = input.readSInt64NoTag()
                }
                5 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    field5 = input.readBoolNoTag()
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): simple_test.Buildersimple_test {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): simple_test.Buildersimple_test {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (field1 != 0) {
                size += WireFormat.getInt32Size(1, field1)
            }
            if (field2 != 0L) {
                size += WireFormat.getInt64Size(2, field2)
            }
            if (field3 != 0) {
                size += WireFormat.getSInt32Size(3, field3)
            }
            if (field4 != 0L) {
                size += WireFormat.getSInt64Size(4, field4)
            }
            if (field5 != false) {
                size += WireFormat.getBoolSize(5, field5)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (field1 != 0) {
                size += WireFormat.getInt32Size(1, field1)
            }
            if (field2 != 0L) {
                size += WireFormat.getInt64Size(2, field2)
            }
            if (field3 != 0) {
                size += WireFormat.getSInt32Size(3, field3)
            }
            if (field4 != 0L) {
                size += WireFormat.getSInt64Size(4, field4)
            }
            if (field5 != false) {
                size += WireFormat.getBoolSize(5, field5)
            }
            return size
        }

    }

}

fun simple_proto_test(): Int{
    val instance = simple_test.Buildersimple_test(100, 200, 300, 400, true).build()
    val buffer = ByteArray(instance.getSizeNoTag())
    val outputStream = CodedOutputStream(buffer)
    instance.writeTo(outputStream)
    val inputStream = CodedInputStream(buffer)
    val receivedMesssage = simple_test.Buildersimple_test(112, 222, 333, 444, false).parseFrom(inputStream).build()

    assert(receivedMesssage.field1 == 100)
    assert(receivedMesssage.field2 == 200L)
    assert(receivedMesssage.field3 == 300)
    assert(receivedMesssage.field4 == 400L)
    assert(receivedMesssage.field5 == true)
    return 1
}