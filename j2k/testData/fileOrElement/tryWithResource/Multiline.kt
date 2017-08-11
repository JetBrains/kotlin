import java.io.*

class C {
    @JvmThrows(IOException::class)
    internal fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            // reading something
            val c = stream.read()
            println(c)
        }
    }
}