import java.io.*

class C {
    internal fun foo(): Int {
        try {
            ByteArrayInputStream(ByteArray(10)).use { stream ->
                // reading something
                val c = stream.read()
                return c
            }
        } catch (e: IOException) {
            println(e)
            return -1
        }

    }
}