import java.io.*

public class C {
    fun foo() {
        try {
            ByteArrayInputStream(ByteArray(10)).use { stream ->
                // reading something
                val c = stream.read()
                println(c)
            }
        } catch (e: IOException) {
            println(e)
        } catch (e: Exception) {
            System.err.println(e)
        }

    }
}