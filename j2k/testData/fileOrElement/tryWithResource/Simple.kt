import java.io.*

public class C {
    throws(IOException::class)
    fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream -> println(stream.read()) }
    }
}