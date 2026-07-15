import java.io.EOFException
import java.io.IOException
import java.sql.SQLException

class ThrowsExample @Throws(IOException::class) constructor() {
    @Throws(IOException::class, SQLException::class)
    fun throwsFromFunction() {
    }

    @get:Throws(EOFException::class)
    val throwsFromGetter: String
        get() = ""

    @Throws(IOException::class)
    fun <T> genericThrows(value: T): T = value
}
