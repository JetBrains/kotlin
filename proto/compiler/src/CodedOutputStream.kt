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
        val res = ByteArray(5)
        var resSize = 0
        do {
            var curByte = (curValue and 127)
            curValue = curValue ushr 7
            if (curValue != 0) {
                curByte = curByte or 128
            }
            res[resSize] = curByte.toByte()
            resSize++
        } while(curValue != 0)
        output.write(res, 0, resSize)
    }

    fun writeVarint64(value: Long) {
        var curValue = value
        val res = ByteArray(10) // we reserve 10 bytes for the cases when value was negative int32/int64
        var resSize = 0
        while(curValue != 0L) {
            var curByte = (curValue and 127L)
            curValue = curValue ushr 7
            if (curValue != 0L) {
                curByte = curByte or 128L
            }

            res[resSize] = curByte.toByte()
            resSize++
        }
        output.write(res, 0, resSize)
    }
}
