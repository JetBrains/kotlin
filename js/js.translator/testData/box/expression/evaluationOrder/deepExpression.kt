// EXPECTED_REACHABLE_NODES: 1298
package foo

var global = ""

fun bar(s: String) {
    global += s
}

fun foo(s: String): String {
    return s + "F"
}

open class A(val s: String) {
    fun me(): A {
        global += s; return this
    }

    open val f = "A"
}

class B(s: String) : A(s) {
    override val f = "B"
}

fun test(b1: Boolean, b2: Boolean): String {
    global = ""
    try {
        bar(
            foo("G") + foo(
                (if (b1) {
                    bar("then1")
                    (try {
                        if (b2) {
                            bar("then2")
                            A("X")
                        } else {
                            bar("else2")
                            B("Y")
                        }
                    } finally {

                    }).me()
                } else {
                    try {
                        bar("try3")
                        bar("else1")
                    } finally {

                    }
                    (if (b2) {
                        bar("then3")
                        B("M")
                    } else {
                        bar("else3")
                        A("N")
                    }).me()
                }).f
            ) + foo("S")
        )
    } finally {

    }

    return global
}

fun box(): String {

    assertEquals(test(false, false), "try3else1else3NGFAFSF")
    assertEquals(test(false, true), "try3else1then3MGFBFSF")
    assertEquals(test(true, false), "then1else2YGFBFSF")
    assertEquals(test(true, true), "then1then2XGFAFSF")
    return "OK"
}