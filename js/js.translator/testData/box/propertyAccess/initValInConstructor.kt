// EXPECTED_REACHABLE_NODES: 495
package foo

class A {
    val a: Int
    val aa: Int
        get() {
            return field
        }

    var aR = 0
    var aaR = 0
    init {
        a = 1
        aa = 2

        aR = a
        aaR = aa
    }
}

class B {
    private val a: Int
    private val aa: Int
        get() {
            return field
        }

    var aR = 0
    var aaR = 0
    init {
        a = 3
        aa = 4

        aR = a
        aaR = aa
    }
}

fun box(): String {
    val a = A()

    if (a.a != 1) return "a.a != 1, it: ${a.a}"
    if (a.aa != 2) return "a.aa != 2, it: ${a.aa}"
    if (a.aR != 1) return "a.aR != 1, it: ${a.aR}"
    if (a.aaR != 2) return "a.aaR != 2, it: ${a.aaR}"

    val b = B()
    if (b.aR != 3) return "b.aR != 3, it: ${b.aR}"
    if (b.aaR != 4) return "b.aaR != 4, it: ${b.aaR}"

    return "OK"
}