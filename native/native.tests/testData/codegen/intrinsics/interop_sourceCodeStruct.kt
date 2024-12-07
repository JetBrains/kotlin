@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.internal.InternalForKotlinNative::class)

import kotlinx.cinterop.*
import kotlinx.cinterop.internal.*
import kotlin.test.*

// Just making sure this doesn't get accidentally forbidden or otherwise broken.
// Used by auto-generated code, user-defined structs should be declared via
// structs in C headers or .def files instead.

@CStruct("struct { int p0; int p1; }")
class S(rawPtr: NativePtr) : CStructVar(rawPtr) {

    companion object : CStructVar.Type(8, 4)

    var x: Int
        get() = memberAt<IntVar>(0).value
        set(value) {
            memberAt<IntVar>(0).value = value
        }

    var y: Int
        get() = memberAt<IntVar>(4).value
        set(value) {
            memberAt<IntVar>(4).value = value
        }
}

fun box(): String {
    memScoped {
        val s = alloc<S>()

        s.x = 123
        assertEquals(123, s.x)
        assertEquals(123, s.ptr.reinterpret<IntVar>()[0])

        s.y = 321
        assertEquals(321, s.y)
        assertEquals(321, s.ptr.reinterpret<IntVar>()[1])
    }
    return "OK"
}
