class MessageRepeatedZigZag private constructor (var int: IntArray, var long: LongArray) {
    //========== Properties ===========
    //repeated sint32 int = 1

    //repeated sint64 long = 2

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //repeated sint32 int = 1
        if (int.size > 0) {
            output.writeTag(1, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (int.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < int.size) {
                        arraySize += WireFormat.getSInt32SizeNoTag(int[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < int.size) {
                    output.writeSInt32NoTag (int[i])
                    i += 1
                }
            } while(false)
        }

        //repeated sint64 long = 2
        if (long.size > 0) {
            output.writeTag(2, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (long.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < long.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(long[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < long.size) {
                    output.writeSInt64NoTag (long[i])
                    i += 1
                }
            } while(false)
        }

    }

    fun mergeWith (other: MessageRepeatedZigZag) {
        int = int.plus((other.int))
        long = long.plus((other.long))
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(IntArray(0), LongArray(0))
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(IntArray(0), LongArray(0))
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (int.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < int.size) {
                    arraySize += WireFormat.getSInt32SizeNoTag(int[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (long.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < long.size) {
                    arraySize += WireFormat.getSInt64SizeNoTag(long[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (int.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < int.size) {
                    arraySize += WireFormat.getSInt32SizeNoTag(int[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (long.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < long.size) {
                    arraySize += WireFormat.getSInt64SizeNoTag(long[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        return size
    }

    //========== Builder ===========
    class BuilderMessageRepeatedZigZag constructor (var int: IntArray, var long: LongArray) {
        //========== Properties ===========
        //repeated sint32 int = 1
        fun setInt(value: IntArray): MessageRepeatedZigZag.BuilderMessageRepeatedZigZag {
            int = value
            return this
        }
        fun setintByIndex(index: Int, value: Int): MessageRepeatedZigZag.BuilderMessageRepeatedZigZag {
            int[index] = value
            return this
        }

        //repeated sint64 long = 2
        fun setLong(value: LongArray): MessageRepeatedZigZag.BuilderMessageRepeatedZigZag {
            long = value
            return this
        }
        fun setlongByIndex(index: Int, value: Long): MessageRepeatedZigZag.BuilderMessageRepeatedZigZag {
            long[index] = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //repeated sint32 int = 1
            if (int.size > 0) {
                output.writeTag(1, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (int.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < int.size) {
                            arraySize += WireFormat.getSInt32SizeNoTag(int[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < int.size) {
                        output.writeSInt32NoTag (int[i])
                        i += 1
                    }
                } while(false)
            }

            //repeated sint64 long = 2
            if (long.size > 0) {
                output.writeTag(2, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (long.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < long.size) {
                            arraySize += WireFormat.getSInt64SizeNoTag(long[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < long.size) {
                        output.writeSInt64NoTag (long[i])
                        i += 1
                    }
                } while(false)
            }

        }

        //========== Mutating methods ===========
        fun build(): MessageRepeatedZigZag {
            val res = MessageRepeatedZigZag(int, long)
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
                    val expectedByteSize = input.readInt32NoTag()
                    var newArray = IntArray(0)
                    var readSize = 0
                    do {
                        var i = 0
                        while(readSize < expectedByteSize) {
                            var tmp = IntArray(1)
                            tmp[0] = input.readSInt32NoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getSInt32SizeNoTag(tmp[0])
                        }
                        int = newArray
                    } while (false)
                }
                2 -> {
                    if (wireType.id != WireType.LENGTH_DELIMITED.id) {
                        errorCode = 1
                        return false
                    }
                    val expectedByteSize = input.readInt32NoTag()
                    var newArray = LongArray(0)
                    var readSize = 0
                    do {
                        var i = 0
                        while(readSize < expectedByteSize) {
                            var tmp = LongArray(1)
                            tmp[0] = input.readSInt64NoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getSInt64SizeNoTag(tmp[0])
                        }
                        long = newArray
                    } while (false)
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): MessageRepeatedZigZag.BuilderMessageRepeatedZigZag {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): MessageRepeatedZigZag.BuilderMessageRepeatedZigZag {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (int.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < int.size) {
                        arraySize += WireFormat.getSInt32SizeNoTag(int[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (long.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < long.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(long[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (int.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < int.size) {
                        arraySize += WireFormat.getSInt32SizeNoTag(int[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (long.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < long.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(long[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            return size
        }

    }

}

fun compareIntArrays(lhs: IntArray, rhs: IntArray): Boolean {
    if (lhs.size != rhs.size) {
        return false
    }

    var i = 0
    while (i < lhs.size) {
        if (lhs[i] != rhs[i]) {
            return false
        }
        i += 1
    }
    return true
}

fun compareLongArrays(lhs: LongArray, rhs: LongArray): Boolean {
    if (lhs.size != rhs.size) {
        return false
    }

    var i = 0
    while (i < lhs.size) {
        if (lhs[i] != rhs[i]) {
            return false
        }
        i += 1
    }
    return true
}

fun compareBooleanArrays(lhs: BooleanArray, rhs: BooleanArray): Boolean {
    if (lhs.size != rhs.size) {
        return false
    }

    var i = 0
    while (i < lhs.size) {
        if (lhs[i] != rhs[i]) {
            return false
        }
        i += 1
    }
    return true
}

fun compareRepeatedZigZags(kt1: MessageRepeatedZigZag, kt2: MessageRepeatedZigZag): Boolean {
    return  compareIntArrays(kt1.int, kt2.int) and
            compareLongArrays(kt1.long, kt2.long)
}

fun checkRepZZSerializationIdentity(msg: MessageRepeatedZigZag): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(
            IntArray(0),
            LongArray(0)
        ).parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareRepeatedZigZags(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

object Rng {
    var rngState = 0.6938893903907228

    val point = 762939453125
    fun rng(): Double {
        val res = rngState - rngState.toInt().toDouble()
        rngState *= point.toDouble()
        return res
    }
}

fun nextInt(minVal: Int, maxVal: Int): Int {
    return (Rng.rng() * (maxVal - minVal + 1).toDouble()).toInt()
}

fun nextLong(): Long {
    val lowerHalf = nextInt(-2147483647, 2147483647)
    val upperHalf = nextInt(-2147483647, 2147483647)
    return (upperHalf.toLong() shl 32) + lowerHalf.toLong()
}

fun generateIntArray(): IntArray {
    val size = nextInt(0, 10000)
    val arr = IntArray(size)
    var i = 0
    while (i < size) {
        arr[i] = nextInt(-2147483647, 2147483647)
        i += 1
    }
    return arr
}

fun generateLongArray(): LongArray {
    val size = nextInt(0, 10000)
    val arr = LongArray(size)
    var i = 0
    while (i < size) {
        arr[i] = nextLong()
        i += 1
    }
    return arr
}

fun generateRandomMessage(): MessageRepeatedZigZag {
    return MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(
            generateIntArray(),
            generateLongArray()
    ).build()
}

fun testRepZigZag(): Int {
    var i = 0
    val testRuns = 100
    while (i < 100) {
        val msg = generateRandomMessage()
        if (checkRepZZSerializationIdentity(msg) == 1) {
            return 1
        }
        i += 1
    }
    return 0
}

fun testArraysOfMinValues(): Int {
    val intArr = IntArray(100)
    var i = 0
    while (i < 100) {
        intArr[i] = -2147483647
        i += 1
    }

    val longArr = LongArray(100)
    i = 0
    while (i < 100) {
        longArr[i] = -9223372036854775807L
        i += 1
    }

    val msg = MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(intArr, longArr).build()
    return checkRepZZSerializationIdentity(msg)
}

fun testArraysOfMaxValues(): Int {
    val intArr = IntArray(100)
    var i = 0
    while (i < 100) {
        intArr[i] = 2147483647
        i += 1
    }

    val longArr = LongArray(100)
    i = 0
    while (i < 100) {
        longArr[i] = 9223372036854775807L
        i += 1
    }

    val msg = MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(intArr, longArr).build()
    return checkRepZZSerializationIdentity(msg)
}

fun testArraysOfDefaultValues(): Int {
    val intArr = IntArray(100)

    val longArr = LongArray(100)

    val msg = MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(intArr, longArr).build()
    return checkRepZZSerializationIdentity(msg)
}
