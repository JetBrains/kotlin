import java.io.*

public class C {
    fun foo(): Int {
        try {
            return FileInputStream("foo").use { stream ->
                // reading something
                val c = stream.read()
                c
            }
        } catch (e: IOException) {
            System.out.println(e)
            return -1
        }

    }
}