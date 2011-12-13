namespace std

namespace io {
    import java.io.*
    import java.nio.charset.*

    inline fun print(message : Any?) { System.out?.print(message) }
    inline fun print(message : Int) { System.out?.print(message) }
    inline fun print(message : Long) { System.out?.print(message) }
    inline fun print(message : Byte) { System.out?.print(message) }
    inline fun print(message : Short) { System.out?.print(message) }
    inline fun print(message : Char) { System.out?.print(message) }
    inline fun print(message : Boolean) { System.out?.print(message) }
    inline fun print(message : Float) { System.out?.print(message) }
    inline fun print(message : Double) { System.out?.print(message) }
    inline fun print(message : CharArray) { System.out?.print(message) }

    inline fun println(message : Any?) { System.out?.println(message) }
    inline fun println(message : Int) { System.out?.println(message) }
    inline fun println(message : Long) { System.out?.println(message) }
    inline fun println(message : Byte) { System.out?.println(message) }
    inline fun println(message : Short) { System.out?.println(message) }
    inline fun println(message : Char) { System.out?.println(message) }
    inline fun println(message : Boolean) { System.out?.println(message) }
    inline fun println(message : Float) { System.out?.println(message) }
    inline fun println(message : Double) { System.out?.println(message) }
    inline fun println(message : CharArray) { System.out?.println(message) }
    inline fun println() { System.out?.println() }

    private val stdin : BufferedReader = BufferedReader(InputStreamReader(object : InputStream() {
        override fun read() : Int {
            return System.`in`?.read() ?: -1
        }

        override fun reset() {
            System.`in`?.reset()
        }

        override fun read(b: ByteArray?): Int {
            return System.`in`?.read(b) ?: -1
        }

        override fun close() {
            System.`in`?.close()
        }

        override fun mark(readlimit: Int) {
            System.`in`?.mark(readlimit)
        }

        override fun skip(n: Long): Long {
            return System.`in`?.skip(n) ?: -1.lng
        }

        override fun available(): Int {
            return System.`in`?.available() ?: 0
        }

        override fun markSupported(): Boolean {
            return System.`in`?.markSupported() ?: false
        }

        override fun read(b: ByteArray?, off: Int, len: Int): Int {
            return System.`in`?.read(b, off, len) ?: -1
        }
    }))

    inline fun readLine() : String? = stdin.readLine()

    fun InputStream.iterator() : ByteIterator =
        object: ByteIterator() {
            override val hasNext : Boolean
                get() = available() > 0

            override fun nextByte() = read().byt
        }

    inline fun InputStream.buffered(bufferSize: Int) = BufferedInputStream(this, bufferSize)

    inline val InputStream.reader : InputStreamReader
        get() = InputStreamReader(this)

    inline val InputStream.bufferedReader : BufferedReader
        get() = BufferedReader(reader)

    inline fun InputStream.reader(charset: Charset) : InputStreamReader  = InputStreamReader(this, charset)

    inline fun InputStream.reader(charsetName: String) = InputStreamReader(this, charsetName)

    inline fun InputStream.reader(charsetDecoder: CharsetDecoder) = InputStreamReader(this, charsetDecoder)

    inline val InputStream.buffered : BufferedInputStream
        get() = if(this is BufferedInputStream) this else BufferedInputStream(this)

//    inline val Reader.buffered : BufferedReader
//        get() = if(this is BufferedReader) this else BufferedReader(this)

    inline fun Reader.buffered(bufferSize: Int) = BufferedReader(this, bufferSize)
}