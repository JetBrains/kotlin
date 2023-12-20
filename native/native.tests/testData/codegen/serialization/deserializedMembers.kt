// MODULE: lib
// FILE: lib.kt

package foo.bar

val sb = StringBuilder()

class R<T> {
    inline fun bar(t: T) {
        sb.appendLine("just a single class: $t")
    }
}

class C {
    inline fun foo() {
        sb.appendLine("first level")
    }

    class D {
        inline fun foo() {
            sb.appendLine("second level")
        }

        class E {
            inline fun foo() {
                sb.appendLine("third levelxz")
            }
        }
    }
}

class C2 {
    inline fun foo() {
        sb.appendLine("inner first level")
    }

    inner class D2 {
        inline fun foo() {
            sb.appendLine("inner second level")
        }

        inner class E2 {
            inline fun foo() {
                sb.appendLine("inner third level")
            }
        }
    }
}

class C3<X> {
    inline fun foo(x: X) {
        sb.appendLine("types first level: $x")
    }

    class D3<X> {
        inline fun foo(x: X) {
            sb.appendLine("types second level $x")
        }

        class E3<X> {
            inline fun foo(x: X) {
                sb.appendLine("types third level $x")
            }
        }
    }
}

class C4<X> {
    inline fun foo(x: X) {
        sb.appendLine("inner types first level: $x")
    }

    inner class D4<Y> {
        inline fun foo(x: X, y: Y) {
            sb.appendLine("inner types second level $x, $y")
        }

        inner class E4<Z> {
            inline fun foo(x: X, y: Y, z: Z) {
                sb.appendLine("inner types third level $x, $y, $z")
            }
        }
    }
}

// MODULE: main(lib)
// FILE: main.kt

import foo.bar.*
import kotlin.test.*

fun box(): String {
    val c = C()
    val d = C.D()
    val e = C.D.E()
    c.foo()
    d.foo()
    e.foo()

    val c2 = C2()
    val d2 = C2().D2()
    val e2 = C2().D2().E2()
    c2.foo()
    d2.foo()
    e2.foo()

    val c3 = C3<Int>()
    val d3 = C3.D3<String>()
    val e3 = C3.D3.E3<Float>()
    c3.foo(13)
    d3.foo("cha-cha-cha")
    e3.foo(1.0f)

    val c4 = C4<Int>()
    val d4 = C4<String>().D4<Int>()
    val e4 = C4<Int>().D4<String>().E4<Int>()
    c4.foo(13)
    d4.foo("cawabunga", 17)
    e4.foo(19, "raqa-taqa", 23)

    assertEquals("""
        first level
        second level
        third levelxz
        inner first level
        inner second level
        inner third level
        types first level: 13
        types second level cha-cha-cha
        types third level 1.0
        inner types first level: 13
        inner types second level cawabunga, 17
        inner types third level 19, raqa-taqa, 23

    """.trimIndent(), sb.toString())

    return "OK"
}