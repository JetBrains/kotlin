/**
 * Created by user on 8/8/16.
 */

class KotlinOutputStream(val buffer: ByteArray) {
    var pos = 0

    fun write (data: ByteArray) {
        write(data, 0, data.size)
    }

    fun write (data: ByteArray, begin: Int, size: Int) {
        for (i in begin..(begin + size - 1)) {
            buffer[pos] = data[i]
            pos += 1
        }
    }
}
