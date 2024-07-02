package base

import base.Endianness.BIG
import base.Endianness.LITTLE

fun ByteArray.getByte(index: Int): Byte = this[index]

fun ByteArray.getByteInt(index: Int): Int = getByte(index).toIntUnsigned()

fun ByteArray.getShortInt(index: Int, endianness: Endianness): Int =
        when (endianness) {
            BIG -> getByteInt(index).shl(8).or(getByteInt(index + 1))
            LITTLE -> getByteInt(index).or(getByteInt(index + 1).shl(8))
        }

fun ByteArray.getShort(index: Int, endianness: Endianness): Short =
        getShortInt(index, endianness).toShort()

fun ByteArray.getInt(index: Int, endianness: Endianness): Int =
        when (endianness) {
            BIG -> getShortInt(index, endianness).shl(16).or(getShortInt(index + 2, endianness))
            LITTLE -> getShortInt(index, endianness).or(getShortInt(index + 2, endianness).shl(16))
        }

fun ByteArray.getIntLong(index: Int, endianness: Endianness): Long =
        getInt(index, endianness).toLongUnsigned()

fun ByteArray.getLong(index: Int, endianness: Endianness): Long =
        when (endianness) {
            BIG -> getIntLong(index, endianness).shl(32).or(getIntLong(index + 4, endianness))
            LITTLE -> getIntLong(index, endianness).or(getIntLong(index + 4, endianness).shl(32))
        }

fun ByteArray.getFloat(index: Int, endianness: Endianness): Float =
        Float.fromBits(getInt(index, endianness))

fun ByteArray.getDouble(index: Int, endianness: Endianness): Double =
        Double.fromBits(getLong(index, endianness))
