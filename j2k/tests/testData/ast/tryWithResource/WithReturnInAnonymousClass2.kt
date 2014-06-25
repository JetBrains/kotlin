import java.io.*

trait I {
    throws(javaClass<IOException>())
    public fun doIt(stream: InputStream): Int
}

public class C {
    throws(javaClass<IOException>())
    fun foo(): Int {
        return FileInputStream("foo").use { stream ->
            bar(object : I {
                throws(javaClass<IOException>())
                override fun doIt(stream: InputStream): Int {
                    return stream.available()
                }
            }, stream)
        }
    }

    throws(javaClass<IOException>())
    fun bar(i: I, stream: InputStream): Int {
        return i.doIt(stream)
    }
}