import java.io.*

public class C {
    throws(javaClass<IOException>())
    fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { stream -> System.out.println(stream.read()) }
    }
}