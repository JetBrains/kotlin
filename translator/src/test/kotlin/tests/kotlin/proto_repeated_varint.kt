class MessageRepeatedVarints private constructor (var int: IntArray, var long: LongArray, var sint: IntArray, var slong: LongArray, var bl: BooleanArray, var uint: IntArray, var ulong: LongArray) {
    //========== Properties ===========
    //repeated int32 int = 1

    //repeated int64 long = 2

    //repeated sint32 sint = 3

    //repeated sint64 slong = 4

    //repeated bool bl = 5

    //repeated uint32 uint = 7

    //repeated uint64 ulong = 8

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //repeated int32 int = 1
        if (int.size > 0) {
            output.writeTag(1, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (int.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < int.size) {
                        arraySize += WireFormat.getInt32SizeNoTag(int[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < int.size) {
                    output.writeInt32NoTag (int[i])
                    i += 1
                }
            } while(false)
        }

        //repeated int64 long = 2
        if (long.size > 0) {
            output.writeTag(2, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (long.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < long.size) {
                        arraySize += WireFormat.getInt64SizeNoTag(long[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < long.size) {
                    output.writeInt64NoTag (long[i])
                    i += 1
                }
            } while(false)
        }

        //repeated sint32 sint = 3
        if (sint.size > 0) {
            output.writeTag(3, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (sint.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < sint.size) {
                        arraySize += WireFormat.getSInt32SizeNoTag(sint[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < sint.size) {
                    output.writeSInt32NoTag (sint[i])
                    i += 1
                }
            } while(false)
        }

        //repeated sint64 slong = 4
        if (slong.size > 0) {
            output.writeTag(4, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (slong.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < slong.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < slong.size) {
                    output.writeSInt64NoTag (slong[i])
                    i += 1
                }
            } while(false)
        }

        //repeated bool bl = 5
        if (bl.size > 0) {
            output.writeTag(5, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (bl.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < bl.size) {
                        arraySize += WireFormat.getBoolSizeNoTag(bl[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < bl.size) {
                    output.writeBoolNoTag (bl[i])
                    i += 1
                }
            } while(false)
        }

        //repeated uint32 uint = 7
        if (uint.size > 0) {
            output.writeTag(7, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (uint.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < uint.size) {
                        arraySize += WireFormat.getUInt32SizeNoTag(uint[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < uint.size) {
                    output.writeUInt32NoTag (uint[i])
                    i += 1
                }
            } while(false)
        }

        //repeated uint64 ulong = 8
        if (ulong.size > 0) {
            output.writeTag(8, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (ulong.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < ulong.size) {
                        arraySize += WireFormat.getUInt64SizeNoTag(ulong[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < ulong.size) {
                    output.writeUInt64NoTag (ulong[i])
                    i += 1
                }
            } while(false)
        }

    }

    fun mergeWith (other: MessageRepeatedVarints) {
        int = int.plus((other.int))
        long = long.plus((other.long))
        sint = sint.plus((other.sint))
        slong = slong.plus((other.slong))
        bl = bl.plus((other.bl))
        uint = uint.plus((other.uint))
        ulong = ulong.plus((other.ulong))
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = MessageRepeatedVarints.BuilderMessageRepeatedVarints(IntArray(0), LongArray(0), IntArray(0), LongArray(0), BooleanArray(0), IntArray(0), LongArray(0))
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = MessageRepeatedVarints.BuilderMessageRepeatedVarints(IntArray(0), LongArray(0), IntArray(0), LongArray(0), BooleanArray(0), IntArray(0), LongArray(0))
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
                    arraySize += WireFormat.getInt32SizeNoTag(int[i])
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
                    arraySize += WireFormat.getInt64SizeNoTag(long[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (sint.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(3, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < sint.size) {
                    arraySize += WireFormat.getSInt32SizeNoTag(sint[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (slong.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < slong.size) {
                    arraySize += WireFormat.getSInt64SizeNoTag(slong[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (bl.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(5, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < bl.size) {
                    arraySize += WireFormat.getBoolSizeNoTag(bl[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (uint.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(7, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < uint.size) {
                    arraySize += WireFormat.getUInt32SizeNoTag(uint[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (ulong.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(8, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < ulong.size) {
                    arraySize += WireFormat.getUInt64SizeNoTag(ulong[i])
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
                    arraySize += WireFormat.getInt32SizeNoTag(int[i])
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
                    arraySize += WireFormat.getInt64SizeNoTag(long[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (sint.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(3, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < sint.size) {
                    arraySize += WireFormat.getSInt32SizeNoTag(sint[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (slong.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < slong.size) {
                    arraySize += WireFormat.getSInt64SizeNoTag(slong[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (bl.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(5, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < bl.size) {
                    arraySize += WireFormat.getBoolSizeNoTag(bl[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (uint.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(7, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < uint.size) {
                    arraySize += WireFormat.getUInt32SizeNoTag(uint[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (ulong.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(8, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < ulong.size) {
                    arraySize += WireFormat.getUInt64SizeNoTag(ulong[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        return size
    }

    //========== Builder ===========
    class BuilderMessageRepeatedVarints constructor (var int: IntArray, var long: LongArray, var sint: IntArray, var slong: LongArray, var bl: BooleanArray, var uint: IntArray, var ulong: LongArray) {
        //========== Properties ===========
        //repeated int32 int = 1
        fun setInt(value: IntArray): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            int = value
            return this
        }
        fun setintByIndex(index: Int, value: Int): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            int[index] = value
            return this
        }

        //repeated int64 long = 2
        fun setLong(value: LongArray): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            long = value
            return this
        }
        fun setlongByIndex(index: Int, value: Long): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            long[index] = value
            return this
        }

        //repeated sint32 sint = 3
        fun setSint(value: IntArray): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            sint = value
            return this
        }
        fun setsintByIndex(index: Int, value: Int): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            sint[index] = value
            return this
        }

        //repeated sint64 slong = 4
        fun setSlong(value: LongArray): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            slong = value
            return this
        }
        fun setslongByIndex(index: Int, value: Long): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            slong[index] = value
            return this
        }

        //repeated bool bl = 5
        fun setBl(value: BooleanArray): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            bl = value
            return this
        }
        fun setblByIndex(index: Int, value: Boolean): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            bl[index] = value
            return this
        }

        //repeated uint32 uint = 7
        fun setUint(value: IntArray): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            uint = value
            return this
        }
        fun setuintByIndex(index: Int, value: Int): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            uint[index] = value
            return this
        }

        //repeated uint64 ulong = 8
        fun setUlong(value: LongArray): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            ulong = value
            return this
        }
        fun setulongByIndex(index: Int, value: Long): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            ulong[index] = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //repeated int32 int = 1
            if (int.size > 0) {
                output.writeTag(1, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (int.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < int.size) {
                            arraySize += WireFormat.getInt32SizeNoTag(int[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < int.size) {
                        output.writeInt32NoTag (int[i])
                        i += 1
                    }
                } while(false)
            }

            //repeated int64 long = 2
            if (long.size > 0) {
                output.writeTag(2, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (long.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < long.size) {
                            arraySize += WireFormat.getInt64SizeNoTag(long[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < long.size) {
                        output.writeInt64NoTag (long[i])
                        i += 1
                    }
                } while(false)
            }

            //repeated sint32 sint = 3
            if (sint.size > 0) {
                output.writeTag(3, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (sint.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < sint.size) {
                            arraySize += WireFormat.getSInt32SizeNoTag(sint[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < sint.size) {
                        output.writeSInt32NoTag (sint[i])
                        i += 1
                    }
                } while(false)
            }

            //repeated sint64 slong = 4
            if (slong.size > 0) {
                output.writeTag(4, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (slong.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < slong.size) {
                            arraySize += WireFormat.getSInt64SizeNoTag(slong[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < slong.size) {
                        output.writeSInt64NoTag (slong[i])
                        i += 1
                    }
                } while(false)
            }

            //repeated bool bl = 5
            if (bl.size > 0) {
                output.writeTag(5, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (bl.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < bl.size) {
                            arraySize += WireFormat.getBoolSizeNoTag(bl[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < bl.size) {
                        output.writeBoolNoTag (bl[i])
                        i += 1
                    }
                } while(false)
            }

            //repeated uint32 uint = 7
            if (uint.size > 0) {
                output.writeTag(7, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (uint.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < uint.size) {
                            arraySize += WireFormat.getUInt32SizeNoTag(uint[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < uint.size) {
                        output.writeUInt32NoTag (uint[i])
                        i += 1
                    }
                } while(false)
            }

            //repeated uint64 ulong = 8
            if (ulong.size > 0) {
                output.writeTag(8, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (ulong.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < ulong.size) {
                            arraySize += WireFormat.getUInt64SizeNoTag(ulong[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < ulong.size) {
                        output.writeUInt64NoTag (ulong[i])
                        i += 1
                    }
                } while(false)
            }

        }

        //========== Mutating methods ===========
        fun build(): MessageRepeatedVarints {
            val res = MessageRepeatedVarints(int, long, sint, slong, bl, uint, ulong)
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
                            tmp[0] = input.readInt32NoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getInt32SizeNoTag(tmp[0])
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
                            tmp[0] = input.readInt64NoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getInt64SizeNoTag(tmp[0])
                        }
                        long = newArray
                    } while (false)
                }
                3 -> {
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
                        sint = newArray
                    } while (false)
                }
                4 -> {
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
                        slong = newArray
                    } while (false)
                }
                5 -> {
                    if (wireType.id != WireType.LENGTH_DELIMITED.id) {
                        errorCode = 1
                        return false
                    }
                    val expectedByteSize = input.readInt32NoTag()
                    var newArray = BooleanArray(0)
                    var readSize = 0
                    do {
                        var i = 0
                        while(readSize < expectedByteSize) {
                            var tmp = BooleanArray(1)
                            tmp[0] = input.readBoolNoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getBoolSizeNoTag(tmp[0])
                        }
                        bl = newArray
                    } while (false)
                }
                7 -> {
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
                            tmp[0] = input.readUInt32NoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getUInt32SizeNoTag(tmp[0])
                        }
                        uint = newArray
                    } while (false)
                }
                8 -> {
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
                            tmp[0] = input.readUInt64NoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getUInt64SizeNoTag(tmp[0])
                        }
                        ulong = newArray
                    } while (false)
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): MessageRepeatedVarints.BuilderMessageRepeatedVarints {
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
                        arraySize += WireFormat.getInt32SizeNoTag(int[i])
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
                        arraySize += WireFormat.getInt64SizeNoTag(long[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (sint.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(3, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < sint.size) {
                        arraySize += WireFormat.getSInt32SizeNoTag(sint[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (slong.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < slong.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (bl.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(5, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < bl.size) {
                        arraySize += WireFormat.getBoolSizeNoTag(bl[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (uint.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(7, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < uint.size) {
                        arraySize += WireFormat.getUInt32SizeNoTag(uint[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (ulong.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(8, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < ulong.size) {
                        arraySize += WireFormat.getUInt64SizeNoTag(ulong[i])
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
                        arraySize += WireFormat.getInt32SizeNoTag(int[i])
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
                        arraySize += WireFormat.getInt64SizeNoTag(long[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (sint.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(3, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < sint.size) {
                        arraySize += WireFormat.getSInt32SizeNoTag(sint[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (slong.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < slong.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (bl.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(5, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < bl.size) {
                        arraySize += WireFormat.getBoolSizeNoTag(bl[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (uint.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(7, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < uint.size) {
                        arraySize += WireFormat.getUInt32SizeNoTag(uint[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (ulong.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(8, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < ulong.size) {
                        arraySize += WireFormat.getUInt64SizeNoTag(ulong[i])
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

fun compareRepeatedVarints(kt1: MessageRepeatedVarints, kt2: MessageRepeatedVarints): Boolean {
    return  compareIntArrays(kt1.int, kt2.int) and
            compareLongArrays(kt1.long, kt2.long) and
            compareIntArrays(kt1.sint, kt2.sint) and
            compareLongArrays(kt1.slong, kt2.slong) and
            compareBooleanArrays(kt1.bl, kt2.bl) and
            compareIntArrays(kt1.uint, kt2.uint) and
            compareLongArrays(kt1.ulong, kt2.ulong)
}

fun checkRepSerializationIdentity(msg: MessageRepeatedVarints): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = MessageRepeatedVarints.BuilderMessageRepeatedVarints(
                IntArray(0),
                LongArray(0),
                IntArray(0),
                LongArray(0),
                BooleanArray(0),
                IntArray(0),
                LongArray(0)
            ).parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareRepeatedVarints(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}


object Rng {
    var rngState = 0.6938893903907228

    val point = 762939453125
    fun rng(): Double {
        rngState *= point.toDouble()
        rngState -= rngState.toLong().toDouble()
        return rngState
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


fun nextBool(): Boolean {
    val int = nextInt(-2147483647, 2147483647)
    if (int and 1 == 1) {
        return true
    }
    return false
}

fun generateBooleanArray(): BooleanArray {
    val size = nextInt(0, 10000)
    val arr = BooleanArray(size)
    var i = 0
    while (i < size) {
        arr[i] = nextBool()
        i += 1
    }
    return arr
}

fun generateRandomMessage(): MessageRepeatedVarints {
    return MessageRepeatedVarints.BuilderMessageRepeatedVarints(
            generateIntArray(),
            generateLongArray(),
            generateIntArray(),
            generateLongArray(),
            generateBooleanArray(),
            generateIntArray(),
            generateLongArray()
        ).build()
}

fun testRepVarints(): Int {
    var i = 0
    val testRuns = 100
    while (i < testRuns) {
        val msg = generateRandomMessage()
        if (checkRepSerializationIdentity(msg) == 1) {
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

    val boolArr = BooleanArray(100)
    i = 1
    while (i < 100) {
        boolArr[i] = false
        i += 1
    }

    val msg = MessageRepeatedVarints.BuilderMessageRepeatedVarints(
            intArr, longArr, intArr, longArr, boolArr, intArr, longArr
        ).build()
    return checkRepSerializationIdentity(msg)
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

    val boolArr = BooleanArray(100)
    i = 1
    while (i < 100) {
        boolArr[i] = true
        i += 1
    }

    val msg = MessageRepeatedVarints.BuilderMessageRepeatedVarints(
            intArr, longArr, intArr, longArr, boolArr, intArr, longArr
    ).build()
    return checkRepSerializationIdentity(msg)
}

fun testArraysOfDefaultValues(): Int {
    val intArr = IntArray(100)

    val longArr = LongArray(100)

    val boolArr = BooleanArray(100)

    val msg = MessageRepeatedVarints.BuilderMessageRepeatedVarints(
                intArr, longArr, intArr, longArr, boolArr, intArr, longArr
            ).build()

    return checkRepSerializationIdentity(msg)
}
