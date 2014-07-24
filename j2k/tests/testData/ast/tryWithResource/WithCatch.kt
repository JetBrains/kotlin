import java.io.*

public class C {
    fun foo() {
        try {
            FileInputStream("foo").use { stream ->
                // reading something
                val c = stream.read()
                System.out.println(c)
            }
        } catch (e: IOException) {
            System.out.println(e)
        }

    }
}