import java.io.*

public class C {
    throws(javaClass<IOException>())
    fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream ->
            // reading something
            val c = stream.read()
            System.out.println(c)
        }
    }
}