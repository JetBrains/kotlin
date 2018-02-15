import java.io.*

internal interface I {
    @JvmThrows(IOException::class)
    fun doIt(stream: InputStream): Int
}

class C {
    @JvmThrows(IOException::class)
    internal fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            bar(object : I {
                @JvmThrows(IOException::class)
                override fun doIt(stream: InputStream): Int {
                    return stream.available()
                }
            }, stream)
        }
    }

    @JvmThrows(IOException::class)
    internal fun bar(i: I, stream: InputStream): Int {
        return i.doIt(stream)
    }
}