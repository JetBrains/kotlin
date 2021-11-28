import cenums.*
import kotlinx.cinterop.*
import kotlin.test.*

fun main() {
    memScoped {
        val e = alloc<E.Var>()
        e.value = E.C
        assertEquals(E.C, e.value)

        assertFailsWith<NotImplementedError> {
            e.value = TODO()
        }
    }
}