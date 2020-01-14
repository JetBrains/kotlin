import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

interface I {
    @Throws(IOException::class)
    fun doIt(stream: InputStream): Int
}

class C {
    @Throws(IOException::class)
    fun foo(): Int {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            return bar(
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
    fun bar(i: I, stream: InputStream): Int {
        return i.doIt(stream)
    }
}