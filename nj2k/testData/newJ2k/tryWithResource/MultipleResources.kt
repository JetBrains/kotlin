import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class C {
    @Throws(IOException::class)
    internal fun foo() {
        ByteArrayInputStream(ByteArray(10)).use { input ->
            ByteArrayOutputStream().use { output ->
                output.write(input.read())
                output.write(0)
            }
        }
    }
}
