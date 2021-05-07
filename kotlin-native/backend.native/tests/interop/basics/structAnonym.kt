import kotlinx.cinterop.*
import kotlin.test.*
import structAnonym.*

fun test_simple() {
    val c = retByValue()
    c.useContents {
        assertEquals(37, x)
        assertEquals(42L + 0x100000000, b)

        a[0] = 2
        a[1] = 5
        assertEquals(12, sendByValue(this.readValue()))
    }
}

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

fun test_Trivial() {
    get_Trivial()
            .useContents {
                assertEquals(1, a[0])
                assertEquals(4, a[3])
                assertEquals(42, b)
            }
}

fun test_S() {
    get_S()
            .useContents {
                assertEquals('a', a.toInt().toChar())
                assertEquals('x', x.toInt().toChar())
            }
}

fun test_S1() {
    ret_S1()
            .useContents{
                assertEquals('a', first.toInt().toChar())
                assertEquals('s', second.toInt().toChar())
                assertEquals('z', last.toInt().toChar())
                assertEquals('b', b1.toInt().toChar())
                assertEquals(42L, b2)
                assertEquals(3.14F, f)
                assertEquals(11L, Y2.b11)
            }
}

fun test_S2() {
    ret_S2()
            .useContents{
                assertEquals('a', first.toInt().toChar())
                assertEquals('s', second.toInt().toChar())
                assertEquals('z', last.toInt().toChar())
                assertEquals('b', b1.toInt().toChar())
                assertEquals(42L, b2)
                assertEquals(3.14F, f)
                assertEquals(11L, Y2.b11)
            }
}

fun test_S3() {
    ret_S3()
            .useContents{
                assertEquals('a', first.toInt().toChar())
                assertEquals('s', second.toInt().toChar())
                assertEquals('z', last.toInt().toChar())
                assertEquals('b', b1.toInt().toChar())
                assertEquals(42L, b2)
                assertEquals(3.14F, f)
                assertEquals(11L, Y2.b11)
            }
}

fun test_S4() {
    ret_S4()
            .useContents{
                assertEquals('a', first.toInt().toChar())
                assertEquals('s', second.toInt().toChar())
                assertEquals('z', last.toInt().toChar())
                assertEquals('b', b1.toInt().toChar())
                assertEquals(42L, b2)
                assertEquals(3.14F, f)
                assertEquals(11L, Y2.b11)
            }
}

fun main() {
    test_simple()
    test_GLKVector3()
    test_Trivial()
    test_S()

    test_S1()
    test_S2()
    test_S3()
    test_S4()
}
