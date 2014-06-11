import java.io.*

public class C() {
    fun foo() {
        FileInputStream("foo").use { stream ->
            // reading something
            val c = stream.read()
            System.out.println(c)
        }
    }
}