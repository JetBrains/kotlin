package std.io

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

/** Uses the given resource then closes it to ensure its closed again */
inline fun <T: Closeable, R> T.foreach(block: (T)-> R) : R {
  var closed = false
  try {
    return block(this)
  } catch (e: Exception) {
    try {
      this.close()
    } catch (closeException: Exception) {
      // eat the closeException as we are already throwing the original cause
      // and we don't want to mask the real exception
    }
    throw e
  } finally {
    if (!closed) {
      this.close()
    }
  }
}

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

//  inline val Reader.buffered : BufferedReader
//        get() = if(this is BufferedReader) this else BufferedReader(this)

inline fun Reader.buffered(): BufferedReader = if(this is BufferedReader) this else BufferedReader(this)

inline fun Reader.buffered(bufferSize: Int) = BufferedReader(this, bufferSize)

inline fun Reader.foreachLine(block: (String) -> Unit): Unit {
  this.foreach{
    val iter = buffered().lineIterator()
    while (iter.hasNext) {
      val elem = iter.next()
      block(elem)
    }
  }
}

inline fun <T> Reader.useLines(block: (Iterator<String>) -> T): T = this.buffered().foreach<BufferedReader, T>{block(it.lineIterator())}
/**
 * Returns an iterator over each line.
 * <b>Note</b> the caller must close the underlying <code>BufferedReader</code>
 * when the iteration is finished; as the user may not complete the iteration loop (e.g. using a method like find() or any() on the iterator
 * may terminate the iteration early.
 * <br>
 * We suggest you try the method useLines() instead which closes the stream when the processing is complete.
 */
inline fun BufferedReader.lineIterator() : Iterator<String> = LineIterator(this)

protected class LineIterator(val reader: BufferedReader) : Iterator<String> {
  private var nextLine: String? = null

  override val hasNext: Boolean
    get() {
      nextLine = reader.readLine()
      return nextLine != null
    }

  override fun next(): String = nextLine.sure()
}

