import java.io.*

interface I {
    Throws(IOException::class)
    public fun doIt(stream: InputStream): Int
}

public class C {
    Throws(IOException::class)
    fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            bar(object : I {
                Throws(IOException::class)
                override fun doIt(stream: InputStream): Int {
                    return stream.available()
                }
            }, stream)
        }
    }

    Throws(IOException::class)
    fun bar(i: I, stream: InputStream): Int {
        return i.doIt(stream)
    }
}