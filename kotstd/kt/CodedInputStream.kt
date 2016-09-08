/**
 * Created by Dmitry Savvinov on 7/6/16.
 *
 * Hides details of work with Protobuf encoding
 *
 * Note that CodedInputStream reads protobuf-defined types from stream (such as int32, sint32, etc),
 * while CodedOutputStream has methods for writing Kotlin-types (such as Boolean, Int, Long, Short, etc)
 *
 */

// TODO get this out of native translator stdlib
// TODO: refactor correctness checks into readTag
class CodedInputStream(val buffer: ByteArray) {
    val inputStream: KotlinInputStream
    init {
        inputStream = KotlinInputStream(buffer)
    }

    fun mark() {
        inputStream.mark()
    }

    fun reset() {
        inputStream.reset()
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
