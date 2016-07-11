import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by Dmitry Savvinov on 7/6/16.
 *
 * Hides details of work with Protobuf encoding
 *
 * Note that CodedInputStream reads protobuf-defined types from stream (such as int32, sint32, etc),
 * while CodedOutputStream has methods for writing Kotlin-types (such as Boolean, Int, Long, Short, etc)
 *
 */

// TODO: decide, if we want to optimize for performance (then we have to deal with spaghetti-code)
class CodedInputStream(input: java.io.InputStream) {

    val bufferedInput: java.io.BufferedInputStream
    init {
        bufferedInput = java.io.BufferedInputStream(input)  // TODO: Java's realization uses hand-written buffers. Why?
    }

    fun readInt32(expectedFieldNumber: Int): Int {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.VARINT, actualWireType)
        return readRawVarint32()
    }

    // Note that unsigned integer types are stored as their signed counterparts with top bit
    // simply stored in the sign bit - similar to Java's protobuf implementation. Hence, all
    // methods reading unsigned ints simply redirect call to corresponding signed-reading method
    fun readUInt32(expectedFieldNumber: Int): Int {
        return readInt32(expectedFieldNumber)
    }

    fun readInt64(expectedFieldNumber: Int): Long {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, actualWireType, WireType.VARINT)
        return readRawVarint64()
    }

    // See note on unsigned integers implementations above
    fun readUInt64(expectedFieldNumber: Int): Long {
        return readUInt64(expectedFieldNumber)
    }

    fun readBool(expectedFieldNumber: Int): Boolean {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, actualWireType, WireType.VARINT)
        val readValue = readRawVarint32()
        val boolValue = when (readValue) {
            0 -> false
            1 -> true
            else -> throw InvalidProtocolBufferException("Expected boolean-encoding (1 or 0), got $readValue")
        }
        return boolValue
    }

    // Reading enums is like reading one int32 number. Caller is responsible for converting this ordinal to enum-object
    fun readEnum(expectedFieldNumber: Int): Int {
        return readInt32(expectedFieldNumber)
    }

    fun readSInt32(expectedFieldNumber: Int): Int {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.VARINT, actualWireType)
        return readZigZag32()
    }

    fun readSInt64(expectedFieldNumber: Int): Long {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.VARINT, actualWireType)
        return readZigZag64()
    }

    fun readFixed32(expectedFieldNumber: Int): Int {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.FIX_32, actualWireType)
        return readLittleEndianInt()
    }

    fun readSFixed32(expectedFieldNumber: Int): Int {
        return readFixed32(expectedFieldNumber)
    }

    fun readFixed64(expectedFieldNumber: Int): Long {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.FIX_64, actualWireType)
        return readLittleEndianLong()
    }

    fun readSFixed64(expectedFieldNumber: Int): Long {
        return readFixed64(expectedFieldNumber)
    }

    fun readDouble(expectedFieldNumber: Int): Double {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.FIX_64, actualWireType)
        return readLittleEndianDouble()
    }

    fun readFloat(expectedFieldNumber: Int): Float {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.FIX_32, actualWireType)
        return readLittleEndianFloat()
    }

    fun readString(expectedFieldNumber: Int): String {
        val tag = readTag()
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.LENGTH_DELIMITED, actualWireType)
        val length = readRawVarint32()
        val value = String(readRawBytes(length))
        return value
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
            throw InvalidProtocolBufferException(
                    "Error in protocol format: \n " +
                            "Expected field number ${expectedFieldNumber}, got ${actualFieldNumber}")
        }

        if (expectedWireType != actualWireType) {
            throw InvalidProtocolBufferException("Error in protocol format: \n " +
                    "Expected ${expectedWireType.name} type, got ${actualWireType.name}")
        }
    }

    fun readLittleEndianDouble(): Double {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(8))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getDouble(0)
    }

    fun readLittleEndianFloat(): Float {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(4))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getFloat(0)
    }

    fun readLittleEndianInt(): Int {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(8))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getInt(0)
    }

    fun readLittleEndianLong(): Long {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(4))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getLong(0)
    }

    fun readRawBytes(count: Int): ByteArray {
        val ba = ByteArray(count)
        for (i in 0..(count - 1)) {
            ba[i] = bufferedInput.read().toByte()
        }
        return ba
    }

    // reads tag. Note that it returns 0 for the end of message!
    fun readTag(): Int {
        if (isAtEnd()) {
            return 0        // we can safely return 0 as sign of end of message, because 0-tags are illegal
        }
        val tag = readRawVarint32()
        if (tag == 0) {     // if we somehow had read 0-tag, then message is corrupted
            throw InvalidProtocolBufferException("Invalid tag 0")
        }
        return tag
    }

    // reads varint not larger than 32-bit integer according to protobuf varint-encoding
    fun readRawVarint32(): Int {
        var done: Boolean = false
        var result: Int = 0
        var step: Int = 0
        while (!done) {
            val byte: Int = bufferedInput.read()
            result = result or
                    (
                            (byte and VARINT_INFO_BITS_MASK)
                                    shl
                                    (VARINT_INFO_BITS_COUNT * step)
                            )
            step++
            if ((byte and VARINT_UTIL_BIT_MASK) == 0) {
                done = true
            }
        }
        return result
    }

    // reads varint not larger than 64-bit integer according to protobuf varint-encoding
    fun readRawVarint64(): Long {
        var done: Boolean = false
        var result: Long = 0
        var step: Int = 0
        while (!done) {
            val byte: Int = bufferedInput.read()
            result = result or
                    (
                            (byte and VARINT_INFO_BITS_MASK).toLong()
                                    shl
                                    (VARINT_INFO_BITS_COUNT * step)
                            )
            step++
            if ((byte and VARINT_UTIL_BIT_MASK) == 0 || byte == -1) {
                done = true
            }
        }
        return result
    }

    // reads zig-zag encoded integer not larger than 32-bit long
    fun readZigZag32(): Int {
        val value = readRawVarint32()
        return (value shr 1) xor (-(value and 1))   // bit magic for decoding zig-zag number
    }

    // reads zig-zag encoded integer not larger than 64-bit long
    fun readZigZag64(): Long {
        val value = readRawVarint64()
        return (value shr 1) xor (-(value and 1L))  // bit magic for decoding zig-zag number
    }

    // checks if at least one more byte can be read from underlying input stream
    fun isAtEnd(): Boolean {
        bufferedInput.mark(1)
        val byte = bufferedInput.read()
        bufferedInput.reset()
        return byte == -1
    }

    // couple of constants for magic numbers
    val VARINT_INFO_BITS_COUNT: Int = 7
    val VARINT_INFO_BITS_MASK: Int = 0b01111111    // mask for separating lowest 7 bits, where actual information stored
    val VARINT_UTIL_BIT_MASK: Int = 0b10000000     // mask for separating highest bit, that indicates next byte presence

}

