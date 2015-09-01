import java.io.*

public class C {
    Throws(IOException::class)
    fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { input ->
            ByteArrayOutputStream().use { output ->
                output.write(input.read())
                output.write(0)
            }
        }
    }
}