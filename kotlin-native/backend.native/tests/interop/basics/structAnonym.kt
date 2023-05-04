@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)


import kotlinx.cinterop.*
import kotlin.test.*
import structAnonym.*


fun test_GLKVector3() {
    get_GLKVector3().useContents {
        assertEquals(1.0f, x)
        assertEquals(2.0f, g)
        assertEquals(3.0f, p)
        r = 0.1f
        g = 0.2f
        b = 0.3f
        assertEquals(v[0], r)
        assertEquals(v[1], g)
        assertEquals(v[2], b)

        val ret = hash_GLKVector3(this.readValue())
        assertEquals(s + 2f * t + 4f * p , ret)
    }
}

fun test_StructAnonRecordMember_ImplicitAlignment() {
    retByValue_StructAnonRecordMember_ImplicitAlignment()
            .useContents {
                assertEquals(1, a[0])
                assertEquals(4, a[3])
                assertEquals(42, b)
            }
}

fun test_StructAnonRecordMember_ExplicitAlignment() {
    retByValue_StructAnonRecordMember_ExplicitAlignment()
            .useContents {
                assertEquals('a', a.toInt().toChar())
                assertEquals('x', x.toInt().toChar())
            }
}

fun test_StructAnonRecordMember_Nested() {
    retByValue_StructAnonRecordMember_Nested()
            .useContents {
                assertEquals(37, x)
                assertEquals(42, b)
                assertEquals('z', z.toInt().toChar())
                assertEquals(3.14, y)

                a[0] = 3
                a[1] = 5
                assertEquals(3 + 2*5, sendByValue_StructAnonRecordMember_Nested(this.readValue()))
            }
}

fun test_StructAnonym_Complicate() {
    retByValue_StructAnonRecordMember_Complicate()
            .useContents{
                assertEquals('a', first.toInt().toChar())
                assertEquals('s', second.toInt().toChar())
                assertEquals('z', last.toInt().toChar())
                assertEquals('b', b1.toInt().toChar())
                assertEquals(42L, b2)
                assertEquals(314, f)
                assertEquals(11L, Y2.b11)
            }
}

fun test_StructAnonym_Packed() {
    retByValue_StructAnonRecordMember_Packed2()
            .useContents{
                assertEquals('a', first.toInt().toChar())
                assertEquals('s', second.toInt().toChar())
                assertEquals('z', last.toInt().toChar())
                assertEquals('b', b1.toInt().toChar())
                assertEquals(42L, b2)
                assertEquals(314, f)
                assertEquals(11L, Y2.b11)
            }
}

fun test_StructAnonym_PragmaPacked() {
    retByValue_StructAnonRecordMember_PragmaPacked()
            .useContents{
                assertEquals('a', first.toInt().toChar())
                assertEquals('s', second.toInt().toChar())
                assertEquals('z', last.toInt().toChar())
                assertEquals('b', b1.toInt().toChar())
                assertEquals(42L, b2)
                assertEquals(314, f)
                assertEquals(11L, Y2.b11)
            }
}

fun test_StructAnonym_Packed2() {
    retByValue_StructAnonRecordMember_Packed2()
            .useContents{
                assertEquals('a', first.toInt().toChar())
                assertEquals('s', second.toInt().toChar())
                assertEquals('z', last.toInt().toChar())
                assertEquals('b', b1.toInt().toChar())
                assertEquals(42L, b2)
                assertEquals(314, f)
                assertEquals(11L, Y2.b11)
            }
}

fun main() {
    test_GLKVector3()
    test_StructAnonRecordMember_ImplicitAlignment()
    test_StructAnonRecordMember_ExplicitAlignment()
    test_StructAnonRecordMember_Nested()
    test_StructAnonym_Complicate()
    test_StructAnonym_Packed()
    test_StructAnonym_PragmaPacked()
    test_StructAnonym_Packed2()
}
