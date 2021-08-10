import objclib.*
import kotlinx.cinterop.*
import kotlin.test.*

fun main() {
    val instance = Resource()
    instance.async_get_result { message, error ->
        assertEquals("Hello", message)
    }
}
