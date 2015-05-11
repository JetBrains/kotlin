import java.io.*

interface I {
    throws(IOException::class)
    public fun doIt(stream: InputStream): Int
}

public class C {
    throws(IOException::class)
    fun foo(): Int {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            return bar(object : I {
                throws(IOException::class)
                override fun doIt(stream: InputStream): Int {
                    return stream.available()
                }
            }, stream)
        }
    }

    throws(IOException::class)
    fun bar(i: I, stream: InputStream): Int {
        return i.doIt(stream)
    }
}