import java.io.*

public class C {
    throws(javaClass<IOException>())
    fun foo() {
        FileInputStream("foo").use { stream -> System.out.println(stream.read()) }
    }
}