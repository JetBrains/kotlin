package io

import base.Endianness
import java.io.OutputStream

fun OutputStream.write(string: String) {
    write(string.toByteArray(Charsets.UTF_8))
    write(0)
}

fun OutputStream.writeByte(b: Byte) {
    write(b.toInt())
}

fun OutputStream.writeShort(s: Short, endianness: Endianness) {
    when (endianness) {
        Endianness.LITTLE -> {
            write(s.toInt().shr(0).and(0xff))
            write(s.toInt().shr(8).and(0xff))
        }

        Endianness.BIG -> {
            write(s.toInt().shr(8).and(0xff))
            write(s.toInt().shr(0).and(0xff))
        }
    }
}

fun OutputStream.writeInt(i: Int, endianness: Endianness) {
    when (endianness) {
        Endianness.LITTLE -> {
            write(i shr 0 and 0xff)
            write(i shr 8 and 0xff)
            write(i shr 16 and 0xff)
            write(i shr 24 and 0xff)
        }

        Endianness.BIG -> {
            write(i shr 24 and 0xff)
            write(i shr 16 and 0xff)
            write(i shr 8 and 0xff)
            write(i shr 0 and 0xff)
        }
    }
}

fun OutputStream.writeLong(i: Long, endianness: Endianness) {
    when (endianness) {
        Endianness.LITTLE -> {
            write((i shr 0 and 0xff).toInt())
            write((i shr 8 and 0xff).toInt())
            write((i shr 16 and 0xff).toInt())
            write((i shr 24 and 0xff).toInt())
            write((i shr 32 and 0xff).toInt())
            write((i shr 40 and 0xff).toInt())
            write((i shr 48 and 0xff).toInt())
            write((i shr 56 and 0xff).toInt())
        }

        Endianness.BIG -> {
            write((i shr 56 and 0xff).toInt())
            write((i shr 48 and 0xff).toInt())
            write((i shr 40 and 0xff).toInt())
            write((i shr 32 and 0xff).toInt())
            write((i shr 24 and 0xff).toInt())
            write((i shr 16 and 0xff).toInt())
            write((i shr 8 and 0xff).toInt())
            write((i shr 0 and 0xff).toInt())
        }
    }
}

fun OutputStream.writeFloat(f: Float, endianness: Endianness) {
    writeInt(f.toBits(), endianness)
}

fun OutputStream.writeDouble(d: Double, endianness: Endianness) {
    writeLong(d.toBits(), endianness)
}
