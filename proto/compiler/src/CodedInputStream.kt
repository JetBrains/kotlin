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

    data class Field<ValueType> (val fieldNumber: Int, val wireType: WireType, val value: ValueType) // TODO: think about variance

    val bufferedInput: java.io.BufferedInputStream
    init {
        bufferedInput = java.io.BufferedInputStream(input)  // TODO: Java's realization uses hand-written buffers. Why?
    }

    fun readInt32(): Field<Int> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.VARINT)
            throw InvalidProtocolBufferException("Expected Varint tag, got ${wireType.name}")
        val value = readRawVarint32()
        return Field<Int>(fieldNumber, wireType, value)
    }

    // Note that unsigned integer types are stored as their signed counterparts with top bit
    // simply stored in the sign bit - similar to Java's protobuf implementation. Hence, all
    // methods, reading unsigned ints simply redirect call to corresponding signed-reading method
    fun readUInt32(): Field<Int> {
        return readInt32()
    }

    fun readInt64(): Field<Long> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.VARINT)
            throw InvalidProtocolBufferException("Expected Varint tag, got ${wireType.name}")
        val value = readRawVarint64()
        return Field(fieldNumber, wireType, value)
    }

    // See note on unsigned integers implementations above
    fun readUInt64(): Field<Long> {
        return readUInt64()
    }

    fun readBool(): Field<Boolean> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.VARINT)
            throw InvalidProtocolBufferException("Expected Varint tag, got ${wireType.name}")

        val readValue = readRawVarint32()
        val boolValue = when (readValue) {
            0 -> false
            1 -> true
            else -> throw InvalidProtocolBufferException("Expected boolean-encoding (1 or 0), got $readValue")
        }

        return Field(fieldNumber, wireType, boolValue)
    }

    // Reading enums is like reading one int32 number. Caller is responsible for converting this ordinal to enum-object
    fun readEnum(): Field<Int> {
        return readInt32()
    }

    fun readSInt32(): Field<Int> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.VARINT)
            throw InvalidProtocolBufferException("Expected Varint tag, got ${wireType.name}")
        val value = readZigZag32()
        return Field(fieldNumber, wireType, value)
    }

    fun readSInt64(): Field<Long> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.VARINT)
            throw InvalidProtocolBufferException("Expected Varint tag, got ${wireType.name}")
        val value = readZigZag64()
        return Field(fieldNumber, wireType, value)
    }

    fun readFixed32(): Field<Int> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.FIX_32)
            throw InvalidProtocolBufferException("Expected FIX_32 tag, got ${wireType.name}")
        val value = readLittleEndianInt()
        return Field(fieldNumber, wireType, value)
    }

    fun readSFixed32(): Field<Int> {
        return readFixed32()
    }

    fun readFixed64(): Field<Long> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.FIX_64)
            throw InvalidProtocolBufferException("Expected FIX_64 tag, got ${wireType.name}")
        val value = readLittleEndianLong()
        return Field(fieldNumber, wireType, value)
    }

    fun readSFixed64(): Field<Long> {
        return readFixed64()
    }

    fun readDouble(): Field<Double> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.FIX_64)
            throw InvalidProtocolBufferException("Expected FIX_64 tag, got ${wireType.name}")
        val value = readLittleEndianDouble()
        return Field(fieldNumber, wireType, value)
    }

    fun readFloat(): Field<Float> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.FIX_32)
            throw InvalidProtocolBufferException("Expected FIX_64 tag, got ${wireType.name}")
        val value = readLittleEndianFloat()
        return Field(fieldNumber, wireType, value)
    }

    fun readString(): Field<String> {
        val (fieldNumber, wireType) = readAndParseTag()
        if (wireType != WireType.LENGTH_DELIMITED)
            throw InvalidProtocolBufferException("Expected LENGTH_DELMITED tag, got ${wireType.name}")
        val length = readRawVarint32()
        val value = String(readRawBytes(length))
        return Field(fieldNumber, wireType, value)
    }

    // ============ private methods ==================

    private fun readLittleEndianDouble(): Double {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(8))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getDouble(0)
    }

    private fun readLittleEndianFloat(): Float {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(4))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getFloat(0)
    }

    private fun readLittleEndianInt(): Int {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(8))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getInt(0)
    }

    private fun readLittleEndianLong(): Long {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(4))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getLong(0)
    }

    private fun readRawBytes(count: Int): ByteArray {
        val ba = ByteArray(count)
        for (i in 0..(count - 1)) {
            ba[i] = bufferedInput.read().toByte()
        }
        return ba
    }

    private fun readTag(): Int {
        if (isAtEnd()) {
            return 0        // we can safely return 0 as sign of end of message, because 0-tags are illegal
        }
        val tag = readRawVarint32()
        if (tag == 0) {     // if we somehow read 0-tag, then message is corrupted
            throw InvalidProtocolBufferException("Invalid tag 0")
        }
        return tag
    }

    private fun readAndParseTag(): Pair<Int, WireType> {
        val tag = readTag()
        return Pair(WireFormat.getTagFieldNumber(tag), WireFormat.getTagWireType(tag))
    }

    private fun readRawVarint32(): Int {
        var done: Boolean = false
        var result: Int = 0
        var step: Int = 0
        while (!done) {
            val byte: Int = bufferedInput.read()
            result = result or ((byte and 127) shl (7 * step))
            step++
            if ((byte and 128) == 0) {
                done = true
            }
        }
        return result
    }

    private fun readRawVarint64(): Long {
        var done: Boolean = false
        var result: Long = 0
        var step: Int = 0
        while (!done) {
            val byte: Int = bufferedInput.read()
            result = result or ((byte and 127).toLong() shl (7 * step))
            step++
            if ((byte and 128) == 0 || byte == -1) {
                done = true
            }
        }
        return result
    }

    private fun readZigZag32(): Int {
        val value = readRawVarint32()
        return (value shr 1) xor (-(value and 1))
    }

    private fun readZigZag64(): Long {
        val value = readRawVarint64()
        return (value shr 1) xor (-(value and 1L))
    }

    private fun isAtEnd(): Boolean {
        bufferedInput.mark(1)
        val byte = bufferedInput.read()
        bufferedInput.reset()
        return byte == -1
    }
}

