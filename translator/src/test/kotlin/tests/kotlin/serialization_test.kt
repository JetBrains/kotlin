
class KotlinInputStream(val buffer: ByteArray) {
    var pos = 0

    fun read(): Byte {
        pos += 1
        return buffer[pos - 1]
    }

    fun isAtEnd(): Boolean {
        return pos >= buffer.size
    }
}

class KotlinOutputStream(val buffer: ByteArray) {
    var pos = 0

    fun write (data: ByteArray) {
        write(data, 0, data.size)
    }

    fun write (data: ByteArray, begin: Int, size: Int) {
        var i = begin
        while (i < begin + size) {
            buffer[pos] = data[i]
            pos += 1
            i++
        }
    }
}

object WireFormat {
    // couple of constants for magic numbers
    val TAG_TYPE_BITS: Int = 3
    val TAG_TYPE_MASK: Int = (1 shl TAG_TYPE_BITS) - 1
    val VARINT_INFO_BITS_COUNT: Int = 7
    val VARINT_INFO_BITS_MASK: Int = 0b01111111    // mask for separating lowest 7 bits, where actual information stored
    val VARINT_UTIL_BIT_MASK: Int = 0b10000000     // mask for separating highest bit, that indicates next byte presence
    val FIXED_32_BYTE_SIZE: Int = 4
    val FIXED_64_BYTE_SIZE: Int = 8

    fun getTagWireType(tag: Int): WireType {
        return WireType.from((tag and TAG_TYPE_MASK).toByte())
    }

    fun getTagFieldNumber(tag: Int): Int {
        return tag ushr TAG_TYPE_BITS
    }

    // TODO: refactor casts into function overloading as soon as translator will support it
    fun getTagSize(fieldNumber: Int, wireType: WireType): Int {
        return getVarint32Size((fieldNumber shl 3) or wireType.id)
    }

    fun getVarint32Size(value: Int): Int {
        var curValue = value
        var size = 0
        do {
            size += 1
            curValue = curValue ushr VARINT_INFO_BITS_COUNT
        } while (curValue != 0)
        return size
    }

    fun getVarint64Size(value: Long): Int {
        var curValue = value
        var size = 0
        do {
            size += 1
            curValue = curValue ushr VARINT_INFO_BITS_COUNT
        }while (curValue != 0L)
        return size
    }

    fun getZigZag32Size(value: Int): Int {
        return getVarint32Size((value shl 1) xor (value shr 31))
    }

    fun getZigZag64Size(value: Long): Int {
        return getVarint64Size((value shl 1) xor (value shr 63))
    }

    fun getInt32Size(fieldNumber: Int, value: Int): Int {
        return getTagSize(fieldNumber, WireType.VARINT) + getInt32SizeNoTag(value)
    }

    fun getInt32SizeNoTag(value: Int): Int {
        if (value < 0) {
            return getVarint64Size(value.toLong())
        }
        return getVarint32Size(value)
    }

    fun getUInt32Size(fieldNumber: Int, value: Int): Int {
        return getTagSize(fieldNumber, WireType.VARINT) + getUInt32SizeNoTag(value)
    }

    fun getUInt32SizeNoTag(value: Int): Int {
        return getVarint32Size(value)
    }

    fun getInt64Size(fieldNumber: Int, value: Long): Int {
        return getTagSize(fieldNumber, WireType.VARINT) + getUInt64SizeNoTag(value)
    }

    fun getInt64SizeNoTag(value: Long): Int {
        return getVarint64Size(value)
    }

    fun getUInt64Size(fieldNumber: Int, value: Long): Int {
        return getInt64Size(fieldNumber, value)
    }

    fun getUInt64SizeNoTag(value: Long): Int {
        return getVarint64Size(value)
    }

    fun getBoolSize(fieldNumber: Int, value: Boolean): Int {
        val intValue = if (value) 1 else 0
        return getInt32Size(fieldNumber, intValue)
    }

    fun getBoolSizeNoTag(value: Boolean): Int {
        val intValue = if (value) 1 else 0
        return getInt32SizeNoTag(intValue)
    }

    fun getEnumSize(fieldNumber: Int, value: Int): Int {
        return getInt32Size(fieldNumber, value)
    }

    fun getEnumSizeNoTag(value: Int): Int {
        return getInt32SizeNoTag(value)
    }

    fun getSInt32Size(fieldNumber: Int, value: Int): Int {
        return getTagSize(fieldNumber, WireType.VARINT) + getZigZag32Size(value)
    }

    fun getSInt32SizeNoTag(value: Int): Int {
        return getZigZag32Size(value)
    }

    fun getSInt64Size(fieldNumber: Int, value: Long): Int {
        return getTagSize(fieldNumber, WireType.VARINT) + getZigZag64Size(value)
    }

    fun getSInt64SizeNoTag(value: Long): Int {
        return getZigZag64Size(value)
    }

    fun getFixed32Size(fieldNumber: Int, value: Int): Int {
        return getTagSize(fieldNumber, WireType.FIX_32) + FIXED_32_BYTE_SIZE
    }

    fun getFixed32SizeNoTag(value: Int): Int {
        return FIXED_32_BYTE_SIZE
    }

    fun getFixed64Size(fieldNumber: Int, value: Long): Int {
        return getTagSize(fieldNumber, WireType.FIX_64) + FIXED_64_BYTE_SIZE
    }

    fun getFixed64SizeNoTag(value: Long): Int  {
        return FIXED_64_BYTE_SIZE
    }

    fun getDoubleSize(fieldNumber: Int, value: Double): Int {
        return getTagSize(fieldNumber, WireType.FIX_64) + FIXED_64_BYTE_SIZE
    }

    fun getDoubleSizeNoTag(value: Double): Int {
        return FIXED_64_BYTE_SIZE
    }

    fun getFloatSize(fieldNumber: Int, value: Float): Int {
        return getTagSize(fieldNumber, WireType.FIX_32) + FIXED_32_BYTE_SIZE
    }

    fun getFloatSizeNoTag(value: Float): Int {
        return FIXED_32_BYTE_SIZE
    }

    fun getBytesSize(fieldNumber: Int, value: ByteArray): Int {
        if (value.size == 0)
            return 0
        var size = 0
        return value.size + getTagSize(fieldNumber, WireType.LENGTH_DELIMITED) + getVarint32Size(value.size)
    }

    fun getBytesSizeNoTag(value: ByteArray): Int {
        return value.size + getVarint32Size(value.size)
    }
}

class CodedInputStream(val buffer: ByteArray) {
    val inputStream: KotlinInputStream
    init {
        inputStream = KotlinInputStream(buffer)  // TODO: Java's realization uses hand-written buffers. Why?
    }

    fun readInt32(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.VARINT, actualWireType)
        return readInt32NoTag()
    }

    // Note that unsigned integer types are stored as their signed counterparts with top bit
    // simply stored in the sign bit - similar to Java's protobuf implementation. Hence, all
    // methods reading unsigned ints simply redirect call to corresponding signed-reading method
    fun readUInt32(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readUInt32NoTag()
    }

    fun readUInt32NoTag(): Int {
        return readInt32NoTag()
    }

    fun readInt64(expectedFieldNumber: Int): Long {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readInt64NoTag()
    }

    // See note on unsigned integers implementations above
    fun readUInt64(expectedFieldNumber: Int): Long {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readUInt64NoTag()
    }

    fun readUInt64NoTag(): Long {
        return readInt64NoTag()
    }

    fun readBool(expectedFieldNumber: Int): Boolean {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readBoolNoTag()
    }

    fun readBoolNoTag(): Boolean {
        val readValue = readInt32NoTag()
        val boolValue = when (readValue) {
            0 -> false
            1 -> true
            else -> false
        }
        return boolValue
    }

    // Reading enums is like reading one int32 number. Caller is responsible for converting this ordinal to enum-object
    fun readEnum(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readEnumNoTag()
    }

    fun readEnumNoTag(): Int {
        return readUInt32NoTag()
    }

    fun readSInt32(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readSInt32NoTag()
    }

    fun readSInt32NoTag(): Int {
        return readZigZag32NoTag()
    }

    fun readSInt64(expectedFieldNumber: Int): Long {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readSInt64NoTag()
    }

    fun readSInt64NoTag(): Long {
        return readZigZag64NoTag()
    }
    fun readBytes(expectedFieldNumber: Int): ByteArray {
        val tag = readTag(expectedFieldNumber, WireType.LENGTH_DELIMITED)
        return readBytesNoTag()
    }

    fun readBytesNoTag(): ByteArray {
        val length = readInt32NoTag()
        return readRawBytes(length)
    }

    /** ============ Utility methods ==================
     *  They are left non-private for cases when one wants to implement her/his own protocol format.
     *  Then she/he can re-use low-level methods for operating with raw values, that are not annotated with Protobuf tags.
     */

    fun checkFieldCorrectness(
            expectedFieldNumber: Int,
            actualFieldNumber: Int,
            expectedWireType: WireType,
            actualWireType: WireType) {
        if (expectedFieldNumber != actualFieldNumber) {
            return
        }

        if (expectedWireType.id != actualWireType.id) {
            return
        }
    }

    fun readRawBytes(count: Int): ByteArray {
        val ba = ByteArray(count)
        var i = 0
        while (i < count) {
            ba[i] = inputStream.read().toByte()
            i++
        }
        return ba
    }

    // reads tag. Note that it returns 0 for the end of message!
    fun readTag(expectedFieldNumber: Int, expectedWireType: WireType): Int {
        if (isAtEnd()) {
            return 0        // we can safely return 0 as sign of end of message, because 0-tags are illegal
        }
        val tag = readInt32NoTag()
        if (tag == 0) {     // if we somehow had read 0-tag, then message is corrupted
            return 0
        }

        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, expectedWireType, actualWireType)
        return tag
    }

    // reads varint not larger than 32-bit integer according to protobuf varint-encoding
    fun readInt32NoTag(): Int {
        var done: Boolean = false
        var result: Long = 0
        var step: Int = 0
        while (!done) {
            val byte: Int = inputStream.read().toInt()
            result = result or
                    (
                            (byte and WireFormat.VARINT_INFO_BITS_MASK).toLong()
                                    shl
                                    (WireFormat.VARINT_INFO_BITS_COUNT * step)
                            ).toLong()
            step++
            if ((byte and WireFormat.VARINT_UTIL_BIT_MASK) == 0) {
                done = true
            }
        }
        return result.toInt()
    }

    // reads varint not larger than 64-bit integer according to protobuf varint-encoding
    fun readInt64NoTag(): Long {
        var done: Boolean = false
        var result: Long = 0
        var step: Int = 0
        while (!done) {
            val byte: Int = inputStream.read().toInt()
            result = result or
                    (
                            (byte and WireFormat.VARINT_INFO_BITS_MASK).toLong()
                                    shl
                                    (WireFormat.VARINT_INFO_BITS_COUNT * step)
                            )
            step++
            if ((byte and WireFormat.VARINT_UTIL_BIT_MASK) == 0 /* || byte == -1 ???? */) {
                done = true
            }
        }
        return result
    }

    // reads zig-zag encoded integer not larger than 32-bit long
    fun readZigZag32NoTag(): Int {
        val value = readInt32NoTag()
        return (value ushr 1) xor (-(value and 1))   // bit magic for decoding zig-zag number
    }

    // reads zig-zag encoded integer not larger than 64-bit long
    fun readZigZag64NoTag(): Long {
        val value = readInt64NoTag()
        return (value ushr 1) xor (-(value and 1L))  // bit magic for decoding zig-zag number
    }

    // checks if at least one more byte can be read from underlying input stream
    fun isAtEnd(): Boolean {
        return inputStream.isAtEnd()
    }
}

class CodedOutputStream(val buffer: ByteArray) {
    val output = KotlinOutputStream(buffer)

    fun toByteArray(): ByteArray {
        return buffer
    }

    fun writeTag(fieldNumber: Int, type: WireType) {
        val tag = (fieldNumber shl 3) or type.id
        writeRawVarint32(tag)
    }

    fun writeInt32(fieldNumber: Int, value: Int) {
        writeTag(fieldNumber, WireType.VARINT)
        writeInt32NoTag(value)
    }

    fun writeInt32NoTag(value: Int) {
        if (value < 0) {    // sign-extend negative values
            writeRawVarint64(value.toLong())
            return
        }
        writeRawVarint32(value)
    }

    // Note that unsigned integer types are stored as their signed counterparts with top bit
    // simply stored in the sign bit - similar to Java's protobuf implementation. Hence, all
    // methods, writing unsigned ints simply redirect call to corresponding signed-writing method
    fun writeUInt32(fieldNumber: Int, value: Int) {
        writeTag(fieldNumber, WireType.VARINT)
        writeUInt32NoTag(value)
    }

    fun writeUInt32NoTag(value: Int) {
        writeRawVarint32(value)
    }

    fun writeInt64(fieldNumber: Int, value: Long) {
        writeTag(fieldNumber, WireType.VARINT)
        writeInt64NoTag(value)
    }

    fun writeInt64NoTag(value: Long) {
        writeRawVarint64(value)
    }

    // See notes on unsigned integers implementation above
    fun writeUInt64(fieldNumber: Int, value: Long) {
        writeTag(fieldNumber, WireType.VARINT)
        writeUInt64NoTag(value)
    }

    fun writeUInt64NoTag(value: Long) {
        writeRawVarint64(value)
    }

    fun writeBool(fieldNumber: Int, value: Boolean) {
        writeTag(fieldNumber, WireType.VARINT)
        writeBoolNoTag(value)
    }

    fun writeBoolNoTag(value: Boolean) {
        writeRawVarint32(if (value) 1 else 0)
    }

    // Writing enums is like writing one int32 number. Caller is responsible for converting enum-object to ordinal
    fun writeEnum(fieldNumber: Int, value: Int) {
        writeTag(fieldNumber, WireType.VARINT)
        writeEnumNoTag(value)
    }

    fun writeEnumNoTag(value: Int) {
        writeRawVarint32(value)
    }

    fun writeSInt32(fieldNumber: Int, value: Int) {
        writeTag(fieldNumber, WireType.VARINT)
        writeSInt32NoTag(value)
    }

    fun writeSInt32NoTag(value: Int) {
        writeUInt32NoTag((value shl 1) xor (value shr 31))
    }

    fun writeSInt64(fieldNumber: Int, value: Long) {
        writeTag(fieldNumber, WireType.VARINT)
        writeSInt64NoTag(value)
    }

    fun writeSInt64NoTag(value: Long) {
        writeUInt64NoTag((value shl 1) xor (value shr 63))
    }

    fun writeBytes(fieldNumber: Int, value: ByteArray) {
        if (value.size == 0) {
            return
        }
        writeTag(fieldNumber, WireType.LENGTH_DELIMITED)
        writeBytesNoTag(value)
    }

    fun writeBytesNoTag(value: ByteArray) {
        writeRawVarint32(value.size)
        output.write(value)
    }

    /** ============ Utility methods ==================
     *  They are left non-private for cases when one wants to implement her/his own protocol format.
     *  Then she/he can re-use low-level methods for operating with raw values, that are not annotated with Protobuf tags.
     */

    fun writeRawVarint32(value: Int) {
        var curValue: Int = value

        // we have at most 32 information bits. With overhead of 1 bit per 7 bits we need at most 5 bytes for encoding
        val res = ByteArray(5)

        var resSize = 0
        do {
            // encode current 7 bits
            var curByte = (curValue and WireFormat.VARINT_INFO_BITS_MASK)

            // discard encoded bits. Note that unsigned shift is needed for cases with negative numbers
            curValue = curValue ushr WireFormat.VARINT_INFO_BITS_COUNT

            // check if there will be next byte in encoding and set util bit if needed
            if (curValue != 0) {
                curByte = curByte or WireFormat.VARINT_UTIL_BIT_MASK
            }

            res[resSize] = curByte.toByte()
            resSize++
        } while (curValue != 0)
        output.write(res, 0, resSize)
    }

    fun writeRawVarint64(value: Long) {
        var curValue: Long = value

        // we have at most 64 information bits. With overhead of 1 bit per 7 bits we need at most 10 bytes for encoding
        val res = ByteArray(10)

        var resSize = 0
        while(curValue != 0L) {
            // encode current 7 bits
            var curByte = (curValue and WireFormat.VARINT_INFO_BITS_MASK.toLong())

            // discard encoded bits. Note that unsigned shift is needed for cases with negative numbers
            curValue = curValue ushr WireFormat.VARINT_INFO_BITS_COUNT

            // check if there will be next byte and set util bit if needed
            if (curValue != 0L) {
                curByte = curByte or WireFormat.VARINT_UTIL_BIT_MASK.toLong()
            }

            res[resSize] = curByte.toByte()
            resSize++
        }
        output.write(res, 0, resSize)
    }


}

enum class WireType(val id: Int) {
    VARINT(0),              // int32, int64, uint32, uint64, sint32, sint64, bool, enum
    FIX_64(1),              // fixed64, sfixed64, double
    LENGTH_DELIMITED(2),    // string, bytes, embedded messages, packed repeated fields
    START_GROUP(3),         // groups (deprecated)
    END_GROUP(4),           // groups (deprecated)
    FIX_32(5),              // fixed32, sfixed32, float
    UNDEFINED(6);           // indicates error when parsing from Int

    companion object {
        fun from (value: Byte): WireType {
            return when (value) {
                0.toByte() -> VARINT
                1.toByte() -> FIX_64
                2.toByte() -> LENGTH_DELIMITED
                3.toByte() -> START_GROUP
                4.toByte() -> END_GROUP
                5.toByte() -> FIX_32
                else -> UNDEFINED
            }
        }
    }
}

class DirectionRequest private constructor (var command: DirectionRequest.Command, var sid: Int) {
    //========== Properties ===========
    //enum command = 1

    //int32 sid = 2

    var errorCode: Int = 0

    //========== Nested enums declarations ===========
    enum class Command(val ord: Int) {
        stop (0),
        forward (1),
        backward (2),
        left (3),
        right (4),
        Unexpected(5);

        companion object {
            fun fromIntToCommand (ord: Int): Command {
                return when (ord) {
                    0 -> Command.stop
                    1 -> Command.forward
                    2 -> Command.backward
                    3 -> Command.left
                    4 -> Command.right
                    else -> Unexpected
                }
            }
        }
    }
    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //enum command = 1
        if (command != DirectionRequest.Command.fromIntToCommand(0)) {
            output.writeEnum (1, command.ord)
        }

        //int32 sid = 2
        if (sid != 0) {
            output.writeInt32 (2, sid)
        }

    }

    fun mergeWith (other: DirectionRequest) {
        command = other.command
        sid = other.sid
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.fromIntToCommand(0), 0)
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.fromIntToCommand(0), 0)
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (command != DirectionRequest.Command.fromIntToCommand(0)) {
            size += WireFormat.getEnumSize(1, command.ord)
        }
        if (sid != 0) {
            size += WireFormat.getInt32Size(2, sid)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (command != DirectionRequest.Command.fromIntToCommand(0)) {
            size += WireFormat.getEnumSize(1, command.ord)
        }
        if (sid != 0) {
            size += WireFormat.getInt32Size(2, sid)
        }
        return size
    }

    //========== Builder ===========
    class BuilderDirectionRequest constructor (var command: DirectionRequest.Command, var sid: Int) {
        //========== Properties ===========
        //enum command = 1
        fun setCommand(value: DirectionRequest.Command): DirectionRequest.BuilderDirectionRequest {
            command = value
            return this
        }

        //int32 sid = 2
        fun setSid(value: Int): DirectionRequest.BuilderDirectionRequest {
            sid = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //enum command = 1
            if (command != DirectionRequest.Command.fromIntToCommand(0)) {
                output.writeEnum (1, command.ord)
            }

            //int32 sid = 2
            if (sid != 0) {
                output.writeInt32 (2, sid)
            }

        }

        //========== Mutating methods ===========
        fun build(): DirectionRequest {
            return DirectionRequest(command, sid)
        }

        fun parseFieldFrom(input: CodedInputStream): Boolean {
            if (input.isAtEnd()) { return false }
            val tag = input.readInt32NoTag()
            if (tag == 0) { return false }
            val fieldNumber = WireFormat.getTagFieldNumber(tag)
            val wireType = WireFormat.getTagWireType(tag)
            when(fieldNumber) {
                1 -> {
                    if (wireType != WireType.VARINT) { errorCode = 1; return false }
                    command = DirectionRequest.Command.fromIntToCommand(input.readEnumNoTag())
                }
                2 -> {
                    if (wireType != WireType.VARINT) { errorCode = 1; return false }
                    sid = input.readInt32NoTag()
                }
            }
            return true}

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): DirectionRequest.BuilderDirectionRequest {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): DirectionRequest.BuilderDirectionRequest {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (command != DirectionRequest.Command.fromIntToCommand(0)) {
                size += WireFormat.getEnumSize(1, command.ord)
            }
            if (sid != 0) {
                size += WireFormat.getInt32Size(2, sid)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (command != DirectionRequest.Command.fromIntToCommand(0)) {
                size += WireFormat.getEnumSize(1, command.ord)
            }
            if (sid != 0) {
                size += WireFormat.getInt32Size(2, sid)
            }
            return size
        }

    }

}

class DirectionResponse private constructor (var code: Int) {
    //========== Properties ===========
    //int32 code = 1

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //int32 code = 1
        if (code != 0) {
            output.writeInt32 (1, code)
        }

    }

    fun mergeWith (other: DirectionResponse) {
        code = other.code
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = DirectionResponse.BuilderDirectionResponse(0)
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = DirectionResponse.BuilderDirectionResponse(0)
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (code != 0) {
            size += WireFormat.getInt32Size(1, code)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (code != 0) {
            size += WireFormat.getInt32Size(1, code)
        }
        return size
    }

    //========== Builder ===========
    class BuilderDirectionResponse constructor (var code: Int) {
        //========== Properties ===========
        //int32 code = 1
        fun setCode(value: Int): DirectionResponse.BuilderDirectionResponse {
            code = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //int32 code = 1
            if (code != 0) {
                output.writeInt32 (1, code)
            }

        }

        //========== Mutating methods ===========
        fun build(): DirectionResponse {
            return DirectionResponse(code)
        }

        fun parseFieldFrom(input: CodedInputStream): Boolean {
            if (input.isAtEnd()) { return false }
            val tag = input.readInt32NoTag()
            if (tag == 0) { return false }
            val fieldNumber = WireFormat.getTagFieldNumber(tag)
            val wireType = WireFormat.getTagWireType(tag)
            when(fieldNumber) {
                1 -> {
                    if (wireType != WireType.VARINT) { errorCode = 1; return false }
                    code = input.readInt32NoTag()
                }
            }
            return true}

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): DirectionResponse.BuilderDirectionResponse {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): DirectionResponse.BuilderDirectionResponse {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (code != 0) {
                size += WireFormat.getInt32Size(1, code)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (code != 0) {
                size += WireFormat.getInt32Size(1, code)
            }
            return size
        }

    }

}


fun serialization_test1(i: Int): Int {
    val msg = DirectionResponse.BuilderDirectionResponse(i).build()
    val buffer = ByteArray(msg.getSizeNoTag())
    val output = CodedOutputStream(buffer)
    msg.writeTo(output)

    val input = CodedInputStream(buffer)
    val msg2 = DirectionResponse.BuilderDirectionResponse(1).parseFrom(input)

    return msg2.code
}
