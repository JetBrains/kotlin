import java.io.ByteArrayInputStream
import java.io.IOException

class C {
    @Throws(IOException::class)
    internal fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream -> println(stream.read()) }
    }
}