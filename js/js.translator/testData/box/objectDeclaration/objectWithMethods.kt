// EXPECTED_REACHABLE_NODES: 502
package foo

class Test {
    private val a = object {
        fun c() = 3
        fun b() = 2
    }

    fun doTest(): String {
        if (a.c() != 3) {
            return "fail1"
        }
        if (a.b() != 2) {
            return "fail2"
        }
        return "OK"
    }
}

fun box(): String {
    return Test().doTest();
}
