import java.io.*

public class C() {
    fun foo() {
        try {
            FileInputStream("foo").use { stream ->
                // reading something
                val c = stream.read()
                System.out.println(c)
            }
        } finally {
            // dispose something else
        }
    }
}