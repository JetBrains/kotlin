import java.io.*

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