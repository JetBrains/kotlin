import auxiliaryCppSources.*
import kotlin.test.*
import kotlinx.cinterop.*

fun main() {
    assertEquals(name()!!.toKString(), "Hello from C++")
}