// EXPECTED_REACHABLE_NODES: 500
// See KT-6201
package foo

var log = ""

class A() {
    init {
        log += "A"
    }

    object B {
        init {
            log += "B"
        }

        fun bar() {
            log += ".bar"
        }
    }
}

fun box(): String {
    A()
    log += ";"
    A.B.bar()

    assertEquals("A;B.bar", log)

    return "OK"
}