class ThirdMessage private constructor (var second_msg: SecondMessage) {
    //========== Properties ===========
    //message second_msg = 1

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //message second_msg = 1
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        output.writeInt32NoTag(second_msg.getSizeNoTag())
        second_msg.writeTo(output)

    }

    fun mergeWith (other: ThirdMessage) {
        second_msg = other.second_msg
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = ThirdMessage.BuilderThirdMessage(SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build()).build())
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = ThirdMessage.BuilderThirdMessage(SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build()).build())
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (second_msg != SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build()).build()) {
            size += second_msg.getSize(1)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (second_msg != SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build()).build()) {
            size += second_msg.getSize(1)
        }
        return size
    }

    //========== Builder ===========
    class BuilderThirdMessage constructor (var second_msg: SecondMessage) {
        //========== Properties ===========
        //message second_msg = 1
        fun setSecond_msg(value: SecondMessage): ThirdMessage.BuilderThirdMessage {
            second_msg = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //message second_msg = 1
            output.writeTag(1, WireType.LENGTH_DELIMITED)
            output.writeInt32NoTag(second_msg.getSizeNoTag())
            second_msg.writeTo(output)

        }

        //========== Mutating methods ===========
        fun build(): ThirdMessage {
            val res = ThirdMessage(second_msg)
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
                    if (wireType.id != WireType.LENGTH_DELIMITED.id) {
                        errorCode = 1
                        return false
                    }
                    do {
                        val expectedSize = input.readInt32NoTag()
                        second_msg.mergeFromWithSize(input, expectedSize)
                        if (expectedSize != second_msg.getSizeNoTag()) { errorCode = 3; return false }
                    } while(false)
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): ThirdMessage.BuilderThirdMessage {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): ThirdMessage.BuilderThirdMessage {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (second_msg != SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build()).build()) {
                size += second_msg.getSize(1)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (second_msg != SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build()).build()) {
                size += second_msg.getSize(1)
            }
            return size
        }

    }

}

class SecondMessage private constructor (var first_msg: FirstMessage) {
    //========== Properties ===========
    //message first_msg = 1

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //message first_msg = 1
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        output.writeInt32NoTag(first_msg.getSizeNoTag())
        first_msg.writeTo(output)

    }

    fun mergeWith (other: SecondMessage) {
        first_msg = other.first_msg
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build())
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build())
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (first_msg != FirstMessage.BuilderFirstMessage(0).build()) {
            size += first_msg.getSize(1)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (first_msg != FirstMessage.BuilderFirstMessage(0).build()) {
            size += first_msg.getSize(1)
        }
        return size
    }

    //========== Builder ===========
    class BuilderSecondMessage constructor (var first_msg: FirstMessage) {
        //========== Properties ===========
        //message first_msg = 1
        fun setFirst_msg(value: FirstMessage): SecondMessage.BuilderSecondMessage {
            first_msg = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //message first_msg = 1
            output.writeTag(1, WireType.LENGTH_DELIMITED)
            output.writeInt32NoTag(first_msg.getSizeNoTag())
            first_msg.writeTo(output)

        }

        //========== Mutating methods ===========
        fun build(): SecondMessage {
            val res = SecondMessage(first_msg)
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
                    if (wireType.id != WireType.LENGTH_DELIMITED.id) {
                        errorCode = 1
                        return false
                    }
                    do {
                        val expectedSize = input.readInt32NoTag()
                        first_msg.mergeFromWithSize(input, expectedSize)
                        if (expectedSize != first_msg.getSizeNoTag()) { errorCode = 3; return false }
                    } while(false)
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): SecondMessage.BuilderSecondMessage {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): SecondMessage.BuilderSecondMessage {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (first_msg != FirstMessage.BuilderFirstMessage(0).build()) {
                size += first_msg.getSize(1)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (first_msg != FirstMessage.BuilderFirstMessage(0).build()) {
                size += first_msg.getSize(1)
            }
            return size
        }

    }

}

class FirstMessage private constructor (var int_field: Int) {
    //========== Properties ===========
    //int32 int_field = 1

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //int32 int_field = 1
        if (int_field != 0) {
            output.writeInt32 (1, int_field)
        }

    }

    fun mergeWith (other: FirstMessage) {
        int_field = other.int_field
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = FirstMessage.BuilderFirstMessage(0)
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = FirstMessage.BuilderFirstMessage(0)
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (int_field != 0) {
            size += WireFormat.getInt32Size(1, int_field)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (int_field != 0) {
            size += WireFormat.getInt32Size(1, int_field)
        }
        return size
    }

    //========== Builder ===========
    class BuilderFirstMessage constructor (var int_field: Int) {
        //========== Properties ===========
        //int32 int_field = 1
        fun setInt_field(value: Int): FirstMessage.BuilderFirstMessage {
            int_field = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //int32 int_field = 1
            if (int_field != 0) {
                output.writeInt32 (1, int_field)
            }

        }

        //========== Mutating methods ===========
        fun build(): FirstMessage {
            val res = FirstMessage(int_field)
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
                    int_field = input.readInt32NoTag()
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): FirstMessage.BuilderFirstMessage {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): FirstMessage.BuilderFirstMessage {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (int_field != 0) {
                size += WireFormat.getInt32Size(1, int_field)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (int_field != 0) {
                size += WireFormat.getInt32Size(1, int_field)
            }
            return size
        }

    }

}

fun compareThirdMessages(kt1: ThirdMessage, kt2: ThirdMessage): Boolean {
    return kt1.second_msg.first_msg.int_field == kt2.second_msg.first_msg.int_field
}

fun compareSecondMessages(lhs: SecondMessage, rhs: SecondMessage): Boolean {
    return lhs.first_msg.int_field == rhs.first_msg.int_field
}

fun compareFirstMessages(lhs: FirstMessage, rhs: FirstMessage): Boolean {
    return lhs.int_field == rhs.int_field
}

fun buildThirdMessage(value: Int): ThirdMessage.BuilderThirdMessage {
    return ThirdMessage.BuilderThirdMessage(
            SecondMessage.BuilderSecondMessage(
                    FirstMessage.BuilderFirstMessage(
                            value
                    ).build()
            ).build()
        )
}

fun buildSecondMessage(value: Int): SecondMessage.BuilderSecondMessage {
    return SecondMessage.BuilderSecondMessage(
            FirstMessage.BuilderFirstMessage(
                    value
            ).build()
        )
}

fun buildFirstMessage(value: Int): FirstMessage.BuilderFirstMessage {
    return FirstMessage.BuilderFirstMessage(value)
}

fun checkThird(msg: ThirdMessage): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = buildThirdMessage(0).parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareThirdMessages(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

fun checkSecond(msg: SecondMessage): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = buildSecondMessage(0).parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareSecondMessages(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

fun checkFirst(msg: FirstMessage): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = buildFirstMessage(0).parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareFirstMessages(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

fun testThirdCase(): Int {
    val msg = buildThirdMessage(5).build()
    return checkThird(msg)
}

fun testSecondCase(): Int {
    val msg = buildSecondMessage(-31241501).build()
    return checkSecond(msg)
}

fun testFirstCase(): Int {
    val msg = buildFirstMessage(321311430).build()
    return checkFirst(msg)
}

