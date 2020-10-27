import cstructs.*
import kotlinx.cinterop.*
import kotlin.test.*

fun main() {
    produceComplex().useContents {
        assertEquals(ui, 128u)
        ui = 333u
        assertEquals(ui, 333u)

        assertEquals(t.i, 1)
        t.i += 15
        assertEquals(t.i, 16)

        assertEquals(next, null)
        next = this.ptr
        assertEquals(next, this.ptr)
        // Check null pointer because it has Nothing? type.
        next = null
        assertEquals(next, null)

        assertEquals(e, E.R)
        e = E.G
        assertEquals(e, E.G)

        assertEquals(K, nonStrict)
        nonStrict = S
        assertEquals(S, nonStrict)

        assertEquals(arr[0], -51)
        assertEquals(arr[1], -19)
        arr[0] = 51
        arr[1] = 19
        assertEquals(arr[0], 51)
        assertEquals(arr[1], 19)

        assertEquals(true, b)
        b = false
        assertEquals(false, b)

        // Check that subtyping via Nothing-returning functions does not break compiler.
        assertFailsWith<NotImplementedError> {
            ui = TODO()
            t.i = TODO()
            next = TODO()
            e = TODO()
            nonStrict = TODO()
            b = TODO()
        }
    }
    memScoped {
        val packed = alloc<Packed>()
        packed.i = -1
        assertEquals(-1, packed.i)
        packed.e = E.R
        assertEquals(E.R, packed.e)
        // Check that subtyping via Nothing-returning functions does not break compiler.
        assertFailsWith<NotImplementedError> {
            packed.i = TODO()
            packed.e = TODO()
        }
    }
    // Check that generics doesn't break anything.
    checkEnumSubTyping(E.R)
    checkIntSubTyping(630090)
}

fun <T : E> checkEnumSubTyping(e: T) = memScoped {
    val s = alloc<Complex>()
    s.e = e
}

fun <T : Int> checkIntSubTyping(x: T) = memScoped {
    val s = alloc<Trivial>()
    s.i = x
}