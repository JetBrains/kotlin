// EXPECTED_REACHABLE_NODES: 1288
var log = ""

fun bar(): A {
    log += "foo;"
    return A()
}

class A {
    fun f() {
        log += "f;"
    }

    fun g() {
        log += "g;"
    }
}

inline fun <T> with(x: T, a: T.() -> Unit) = x.a()

fun box(): String {
    with(bar()) {
        f()
        g()
    }

    if (log != "foo;f;g;") return "fail: $log"

    return "OK"
}

