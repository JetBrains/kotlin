import java.io.*

public class C {
    throws(javaClass<IOException>())
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