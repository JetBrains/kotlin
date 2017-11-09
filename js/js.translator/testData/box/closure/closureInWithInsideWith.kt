// EXPECTED_REACHABLE_NODES: 1115
// KT-4237 With in with

package foo

class A {
    val ok = "OK"
}

class B

fun <T> with(o: T, body: T.() -> Unit) {
    o.body()
}

fun box(): String {
    var o = ""

    with(A()) {
        with(B()) {
            o = ok
        }
    }

    return o
}
