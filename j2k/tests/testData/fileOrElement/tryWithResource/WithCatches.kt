import java.io.*

public class C {
    fun foo() {
        try {
            ByteArrayInputStream(ByteArray(10)).use { stream ->
                // reading something
                val c = stream.read()
                System.out.println(c)
            }
        } catch (e: IOException) {
            System.out.println(e)
        } catch (e: Exception) {
            System.err.println(e)
        }

    }
}