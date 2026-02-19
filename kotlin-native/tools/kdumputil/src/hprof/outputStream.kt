package hprof

import io.write
import io.writeInt
import io.writeLong
import java.io.OutputStream

fun OutputStream.write(idSize: IdSize) {
    writeInt(idSize.size, HPROF_ENDIANNESS)
}

fun OutputStream.write(profile: Profile) {
    write("JAVA PROFILE 1.0.2")
    write(profile.idSize)
    writeLong(profile.time, HPROF_ENDIANNESS)
    Writer(this, profile.idSize).run {
        write(profile.records) { write(it) }
    }
}
