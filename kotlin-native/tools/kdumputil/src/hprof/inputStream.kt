package hprof

import io.readCString
import io.readInt
import io.readLong
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream

fun InputStream.readProfile(): Profile {
    readCString().also {
        if (it != "JAVA PROFILE 1.0.2") {
            throw IOException("invalid header \"$it\"")
        }
    }
    val idSize = readIdSize()
    val time = readLong(HPROF_ENDIANNESS)
    val records = Reader(PushbackInputStream(this), idSize).readList { readRecord() }
    return Profile(idSize, time, records)
}

fun InputStream.readIdSize(): IdSize = run {
    when (val idSizeInt = readInt(HPROF_ENDIANNESS)) {
        1 -> IdSize.BYTE
        2 -> IdSize.SHORT
        4 -> IdSize.INT
        8 -> IdSize.LONG
        else -> throw IOException("Unknown ID size: $idSizeInt")
    }
}
