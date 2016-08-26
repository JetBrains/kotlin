class LocationResponse private constructor (var locationResponseData: LocationResponse.LocationData, var code: Int, var errorMsg: Int) {
    //========== Properties ===========
    //message locationResponseData = 1

    //int32 code = 2

    //int32 errorMsg = 3

    var errorCode: Int = 0

    //========== Nested classes declarations ===========
    class LocationData private constructor (var x: Int, var y: Int, var angle: Int) {
        //========== Properties ===========
        //int32 x = 1

        //int32 y = 2

        //int32 angle = 3

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //int32 x = 1
            if (x != 0) {
                output.writeInt32 (1, x)
            }

            //int32 y = 2
            if (y != 0) {
                output.writeInt32 (2, y)
            }

            //int32 angle = 3
            if (angle != 0) {
                output.writeInt32 (3, angle)
            }

        }

        fun mergeWith (other: LocationResponse.LocationData) {
            x = other.x
            y = other.y
            angle = other.angle
            this.errorCode = other.errorCode
        }

        fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
            val builder = LocationResponse.LocationData.BuilderLocationData(0, 0, 0)
            mergeWith(builder.parseFromWithSize(input, expectedSize).build())
        }

        fun mergeFrom (input: CodedInputStream) {
            val builder = LocationResponse.LocationData.BuilderLocationData(0, 0, 0)
            mergeWith(builder.parseFrom(input).build())
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (x != 0) {
                size += WireFormat.getInt32Size(1, x)
            }
            if (y != 0) {
                size += WireFormat.getInt32Size(2, y)
            }
            if (angle != 0) {
                size += WireFormat.getInt32Size(3, angle)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (x != 0) {
                size += WireFormat.getInt32Size(1, x)
            }
            if (y != 0) {
                size += WireFormat.getInt32Size(2, y)
            }
            if (angle != 0) {
                size += WireFormat.getInt32Size(3, angle)
            }
            return size
        }

        //========== Builder ===========
        class BuilderLocationData constructor (var x: Int, var y: Int, var angle: Int) {
            //========== Properties ===========
            //int32 x = 1
            fun setX(value: Int): LocationResponse.LocationData.BuilderLocationData {
                x = value
                return this
            }

            //int32 y = 2
            fun setY(value: Int): LocationResponse.LocationData.BuilderLocationData {
                y = value
                return this
            }

            //int32 angle = 3
            fun setAngle(value: Int): LocationResponse.LocationData.BuilderLocationData {
                angle = value
                return this
            }

            var errorCode: Int = 0

            //========== Serialization methods ===========
            fun writeTo (output: CodedOutputStream) {
                //int32 x = 1
                if (x != 0) {
                    output.writeInt32 (1, x)
                }

                //int32 y = 2
                if (y != 0) {
                    output.writeInt32 (2, y)
                }

                //int32 angle = 3
                if (angle != 0) {
                    output.writeInt32 (3, angle)
                }

            }

            //========== Mutating methods ===========
            fun build(): LocationResponse.LocationData {
                val res = LocationResponse.LocationData(x, y, angle)
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
                        x = input.readInt32NoTag()
                    }
                    2 -> {
                        if (wireType.id != WireType.VARINT.id) {
                            errorCode = 1
                            return false
                        }
                        y = input.readInt32NoTag()
                    }
                    3 -> {
                        if (wireType.id != WireType.VARINT.id) {
                            errorCode = 1
                            return false
                        }
                        angle = input.readInt32NoTag()
                    }
                    else -> errorCode = 4
                }
                return true
            }

            fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): LocationResponse.LocationData.BuilderLocationData {
                while(getSizeNoTag() < expectedSize) {
                    parseFieldFrom(input)
                }
                if (getSizeNoTag() > expectedSize) { errorCode = 2 }
                return this
            }

            fun parseFrom(input: CodedInputStream): LocationResponse.LocationData.BuilderLocationData {
                while(parseFieldFrom(input)) {}
                return this
            }

            //========== Size-related methods ===========
            fun getSize(fieldNumber: Int): Int {
                var size = 0
                if (x != 0) {
                    size += WireFormat.getInt32Size(1, x)
                }
                if (y != 0) {
                    size += WireFormat.getInt32Size(2, y)
                }
                if (angle != 0) {
                    size += WireFormat.getInt32Size(3, angle)
                }
                size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
                return size
            }

            fun getSizeNoTag(): Int {
                var size = 0
                if (x != 0) {
                    size += WireFormat.getInt32Size(1, x)
                }
                if (y != 0) {
                    size += WireFormat.getInt32Size(2, y)
                }
                if (angle != 0) {
                    size += WireFormat.getInt32Size(3, angle)
                }
                return size
            }

        }

    }

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //message locationResponseData = 1
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        output.writeInt32NoTag(locationResponseData.getSizeNoTag())
        locationResponseData.writeTo(output)

        //int32 code = 2
        if (code != 0) {
            output.writeInt32 (2, code)
        }

        //int32 errorMsg = 3
        if (errorMsg != 0) {
            output.writeInt32 (3, errorMsg)
        }

    }

    fun mergeWith (other: LocationResponse) {
        locationResponseData = other.locationResponseData
        code = other.code
        errorMsg = other.errorMsg
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = LocationResponse.BuilderLocationResponse(LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build(), 0, 0)
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = LocationResponse.BuilderLocationResponse(LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build(), 0, 0)
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (locationResponseData != LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build()) {
            size += locationResponseData.getSize(1)
        }
        if (code != 0) {
            size += WireFormat.getInt32Size(2, code)
        }
        if (errorMsg != 0) {
            size += WireFormat.getInt32Size(3, errorMsg)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (locationResponseData != LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build()) {
            size += locationResponseData.getSize(1)
        }
        if (code != 0) {
            size += WireFormat.getInt32Size(2, code)
        }
        if (errorMsg != 0) {
            size += WireFormat.getInt32Size(3, errorMsg)
        }
        return size
    }

    //========== Builder ===========
    class BuilderLocationResponse constructor (var locationResponseData: LocationResponse.LocationData, var code: Int, var errorMsg: Int) {
        //========== Properties ===========
        //message locationResponseData = 1
        fun setLocationResponseData(value: LocationResponse.LocationData): LocationResponse.BuilderLocationResponse {
            locationResponseData = value
            return this
        }

        //int32 code = 2
        fun setCode(value: Int): LocationResponse.BuilderLocationResponse {
            code = value
            return this
        }

        //int32 errorMsg = 3
        fun setErrorMsg(value: Int): LocationResponse.BuilderLocationResponse {
            errorMsg = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //message locationResponseData = 1
            output.writeTag(1, WireType.LENGTH_DELIMITED)
            output.writeInt32NoTag(locationResponseData.getSizeNoTag())
            locationResponseData.writeTo(output)

            //int32 code = 2
            if (code != 0) {
                output.writeInt32 (2, code)
            }

            //int32 errorMsg = 3
            if (errorMsg != 0) {
                output.writeInt32 (3, errorMsg)
            }

        }

        //========== Mutating methods ===========
        fun build(): LocationResponse {
            val res = LocationResponse(locationResponseData, code, errorMsg)
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
                        locationResponseData.mergeFromWithSize(input, expectedSize)
                        if (expectedSize != locationResponseData.getSizeNoTag()) { errorCode = 3; return false }
                    } while(false)
                }
                2 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    code = input.readInt32NoTag()
                }
                3 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    errorMsg = input.readInt32NoTag()
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): LocationResponse.BuilderLocationResponse {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): LocationResponse.BuilderLocationResponse {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (locationResponseData != LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build()) {
                size += locationResponseData.getSize(1)
            }
            if (code != 0) {
                size += WireFormat.getInt32Size(2, code)
            }
            if (errorMsg != 0) {
                size += WireFormat.getInt32Size(3, errorMsg)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (locationResponseData != LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build()) {
                size += locationResponseData.getSize(1)
            }
            if (code != 0) {
                size += WireFormat.getInt32Size(2, code)
            }
            if (errorMsg != 0) {
                size += WireFormat.getInt32Size(3, errorMsg)
            }
            return size
        }

    }

}

fun compareLocationDatas(lhs: LocationResponse.LocationData, rhs: LocationResponse.LocationData): Boolean {
    return (lhs.angle == rhs.angle) and (lhs.x == rhs.x) and (lhs.y == rhs.y)
}

fun compareLocationResponses(lhs: LocationResponse, rhs: LocationResponse): Boolean {
    return (lhs.code == rhs.code) and
            (lhs.errorMsg == rhs.errorMsg) and
            compareLocationDatas(lhs.locationResponseData, rhs.locationResponseData)
}

fun buildLocationResponse(code: Int, errorMsg: Int, angle: Int, x: Int, y: Int): LocationResponse.BuilderLocationResponse {
    return LocationResponse.BuilderLocationResponse(
            LocationResponse.LocationData.BuilderLocationData(
                x, y, angle
            ).build(),
            code,
            errorMsg
        )
}

fun checkLocationResponse(msg: LocationResponse): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = buildLocationResponse(0, 0, 0, 0, 0).parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareLocationResponses(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

fun testTrivialLocationResponse(): Int {
    val msg = buildLocationResponse(3125321, -12673123, 4334, 423542673, 234516534).build()
    return checkLocationResponse(msg)
}

fun testMaxLocationResponse(): Int {
    val msg = buildLocationResponse(2147483647, 2147483647, 2147483647, 2147483647, 2147483647).build()
    return checkLocationResponse(msg)
}

fun testMinLocationResponse(): Int {
    val msg = buildLocationResponse(-2147483647, -2147483647, -2147483647, -2147483647, -2147483647).build()
    return checkLocationResponse(msg)
}