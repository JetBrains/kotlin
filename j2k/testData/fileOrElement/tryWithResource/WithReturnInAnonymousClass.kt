import java.io.*

trait I {
    throws(IOException::class)
    public fun doIt(stream: InputStream): Int
}

public class C {
    throws(IOException::class)
    fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            bar(object : I {
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