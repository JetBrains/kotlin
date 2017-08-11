import java.io.*

class C {
    @JvmThrows(IOException::class)
    internal fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream -> println(stream.read()) }
    }
}