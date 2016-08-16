/**
 * Created by user on 8/8/16.
 */

class KotlinInputStream(val buffer: ByteArray) {
    var pos = 0

    fun read(): Byte {
        val result = buffer[pos]
        pos++
        return result
    }

    fun isAtEnd(): Boolean {
        return pos >= buffer.size
    }
}
