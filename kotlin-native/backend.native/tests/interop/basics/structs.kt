@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

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

    memScoped {
        val SIZE = 10
        val flex = alloc(sizeOf<WithFlexibleArray>() + sizeOf<IntVar>() * SIZE, alignOf<WithFlexibleArray>()).reinterpret<WithFlexibleArray>()
        flex.size = SIZE
        for (i in 0 until SIZE) {
            flex.data[i] = i
        }
        assertEquals(SIZE, flex.size)
        for (i in 0 until SIZE) {
            assertEquals(i, flex.data[i])
        }
    }

    memScoped {
        val SIZE = 10
        val flex = alloc(sizeOf<WithZeroSizedArray>() + sizeOf<IntVar>() * SIZE, alignOf<WithZeroSizedArray>()).reinterpret<WithZeroSizedArray>()
        assertEquals(4, sizeOf<WithZeroSizedArray>())
        assertEquals(4, alignOf<WithZeroSizedArray>())
        flex.size = SIZE
        for (i in 0 until SIZE) {
            flex.data[i] = i
        }
        assertEquals(SIZE, flex.size)
        for (i in 0 until SIZE) {
            assertEquals(i, flex.data[i])
        }
    }

    memScoped {
        val SIZE = 10
        assertEquals(8, sizeOf<WithFlexibleArrayWithPadding>())
        assertEquals(8, alignOf<WithFlexibleArrayWithPadding>())
        val flex = alloc(sizeOf<WithFlexibleArrayWithPadding>() + sizeOf<LongVar>() * SIZE, alignOf<WithFlexibleArrayWithPadding>())
                .reinterpret<WithFlexibleArrayWithPadding>()
        fillArray(flex.ptr, SIZE)
        assertEquals(SIZE, flex.size)
        assertEquals('!'.code.toByte(), flex.c);
        for (i in 0 until SIZE) {
            assertEquals((i.toLong() shl 32) or (i.toLong() * 100), flex.data[i])
        }
    }

}

fun <T : E> checkEnumSubTyping(e: T) = memScoped {
    val s = alloc<Complex>()
    s.e = e
}

fun <T : Int> checkIntSubTyping(x: T) = memScoped {
    val s = alloc<Trivial>()
    s.i = x
}
