// ERROR: 'return' is not allowed here
// ERROR: A 'return' expression required in a function with a block body ('{...}')
import java.io.*

public class C {
    fun foo(): Int {
        try {
            ByteArrayInputStream(ByteArray(10)).use { stream ->
                // reading something
                val c = stream.read()
                return c
            }
        } catch (e: IOException) {
            System.out.println(e)
            return -1
        }

    }
}