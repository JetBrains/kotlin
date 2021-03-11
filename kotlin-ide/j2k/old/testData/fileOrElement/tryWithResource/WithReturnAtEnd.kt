import java.io.*

class C {
    internal fun foo(): Int {
        try {
            ByteArrayInputStream(ByteArray(10)).use { stream ->
                // reading something
                return stream.read()
            }
        } catch (e: IOException) {
            println(e)
            return -1
        }

    }
}