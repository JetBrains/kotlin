import java.io.*

public class C {
    Throws(IOException::class)
    fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            // reading something
            val c = stream.read()
            println(c)
        }
    }
}