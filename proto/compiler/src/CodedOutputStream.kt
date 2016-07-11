import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by user on 7/6/16.
 */

class CodedOutputStream(val output: java.io.OutputStream) {
    fun writeTag(fieldNumber: Int, type: WireType) {
        val tag = (fieldNumber shl 3) or type.ordinal
        writeVarint32(tag)
    }

    fun writeInt32(fieldNumber: Int, value: Int) {
        writeTag(fieldNumber, WireType.VARINT)
        writeVarint32(value)
    }

    // Note that unsigned integer types are stored as their signed counterparts with top bit
    // simply stored in the sign bit - similar to Java's protobuf implementation. Hence, all
    // methods, writing unsigned ints simply redirect call to corresponding signed-writing method
    fun writeUInt32(fieldNumber: Int, value: Int) {
        writeInt32(fieldNumber, value)
    }

    fun writeInt64(fieldNumber: Int, value: Long) {
        writeTag(fieldNumber, WireType.VARINT)
        writeVarint64(value)
    }

    // See notes on unsigned integers implementation above
    fun writeUIn64(fieldNumber: Int, value: Long) {
        writeInt64(fieldNumber, value)
    }

    fun writeBool(fieldNumber: Int, value: Boolean) {
        writeInt32(fieldNumber, if (value) 1 else 0)
    }

    // Writing enums is like writing one int32 number. Caller is responsible for converting enum-object to ordinal
    fun writeEnum(fieldNumber: Int, value: Int) {
        writeInt32(fieldNumber, value)
    }

    fun writeSInt32(fieldNumber: Int, value: Int) {
        writeInt32(fieldNumber, (value shl 1) xor (value shr 31))
    }

    fun writeSInt64(fieldNumber: Int, value: Long) {
        writeInt64(fieldNumber, (value shl 1) xor (value shr 31))
    }

    fun writeFixed32(fieldNumber: Int, value: Int) {
        writeTag(fieldNumber, WireType.FIX_32)
        writeLittleEndian(value)
    }

    // See notes on unsigned integers implementation above
    fun writeSFixed32(fieldNumber: Int, value: Int) {
        writeFixed32(fieldNumber, value)
    }

    fun writeFixed64(fieldNumber: Int, value: Long) {
        writeTag(fieldNumber, WireType.FIX_64)
        writeLittleEndian(value)
    }

    // See notes on unsigned integers implementation above
    fun writeSFixed64(fieldNumber: Int, value: Long) {
        writeFixed64(fieldNumber, value)
    }

    fun writeDouble(fieldNumber: Int, value: Double) {
        writeTag(fieldNumber, WireType.FIX_64)
        writeLittleEndian(value)
    }

    fun writeFloat(fieldNumber: Int, value: Float) {
        writeTag(fieldNumber, WireType.FIX_32)
        writeLittleEndian(value)
    }

    fun writeString(fieldNumber: Int, value: String) {
        writeTag(fieldNumber, WireType.LENGTH_DELIMITED)
        writeVarint32(value.length)
        output.write(value.toByteArray())
    }

    fun writeLittleEndian(value: Int) {
        val bb = ByteBuffer.allocate(4)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(value)
        output.write(bb.array())
    }

    fun writeLittleEndian(value: Long) {
        val bb = ByteBuffer.allocate(8)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putLong(value)
        output.write(bb.array())
    }

    fun writeLittleEndian(value: Double) {
        val bb = ByteBuffer.allocate(8)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putDouble(value)
        output.write(bb.array())
    }

    fun writeLittleEndian(value: Float) {
        val bb = ByteBuffer.allocate(4)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putFloat(value)
        output.write(bb.array())
    }

    fun writeVarint32(value: Int) {
        var curValue = value

        // we have at most 32 information bits. With overhead of 1 bit per 7 bits we need at most 5 bytes for encoding
        val res = ByteArray(5)

        var resSize = 0
        do {
            // encode current 7 bits
            var curByte = (curValue and VARINT_INFO_BITS_MASK)

            // discard encoded bits. Note that unsigned shift is needed for cases with negative numbers
            curValue = curValue ushr VARINT_INFO_BITS_COUNT

            // check if there will be next byte in encoding and set util bit if needed
            if (curValue != 0) {
                curByte = curByte or VARINT_UTIL_BIT_MASK
            }

            res[resSize] = curByte.toByte()
            resSize++
        } while(curValue != 0)
        output.write(res, 0, resSize)
    }

    fun writeVarint64(value: Long) {
        var curValue = value

        // we have at most 64 information bits. With overhead of 1 bit per 7 bits we need at most 10 bytes for encoding
        val res = ByteArray(10)

        var resSize = 0
        while(curValue != 0L) {
            // encode current 7 bits
            var curByte = (curValue and VARINT_INFO_BITS_MASK.toLong())

            // discard encoded bits. Note that unsigned shift is needed for cases with negative numbers
            curValue = curValue ushr VARINT_INFO_BITS_COUNT

            // check if there will be next byte and set util bit if needed
            if (curValue != 0L) {
                curByte = curByte or VARINT_UTIL_BIT_MASK.toLong()
            }

            res[resSize] = curByte.toByte()
            resSize++
        }
        output.write(res, 0, resSize)
    }

    // couple of constants for magic numbers
    val VARINT_INFO_BITS_COUNT: Int = 7
    val VARINT_INFO_BITS_MASK: Int = 0b01111111    // mask for separating lowest 7 bits, where actual information stored
    val VARINT_UTIL_BIT_MASK: Int = 0b10000000     // mask for separating highest bit, that indicates next byte presence
}
