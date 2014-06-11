import java.io.*

trait I {
    public fun doIt(stream: InputStream): Int
}

public class C() {
    fun foo(): Int {
        return FileInputStream("foo").use { stream ->
            bar(object : I {
                override fun doIt(stream: InputStream): Int {
                    return stream.available()
                }
            }, stream)
        }
    }

    fun bar(i: I, stream: InputStream): Int {
        return i.doIt(stream)
    }
}