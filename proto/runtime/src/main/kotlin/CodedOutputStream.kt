/**
 * Created by user on 7/6/16.
 */

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
