import java.io.*

public class C() {
    fun foo() {
        FileInputStream("foo").use { stream -> System.out.println(stream.read()) }
    }
}