import java.io.*

internal interface I {
    @Throws(IOException::class)
    fun doIt(stream: InputStream): Int
}

class C {
    @Throws(IOException::class)
    internal fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            bar(
                    object : I {
                        @Throws(IOException::class)
                        override fun doIt(stream: InputStream): Int {
                            return stream.available()
                        }
                    },
                    stream
            )
        }
    }

    @Throws(IOException::class)
    internal fun bar(i: I, stream: InputStream): Int {
        return i.doIt(stream)
    }
}
