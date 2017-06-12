// EXPECTED_REACHABLE_NODES: 493
package foo

class Test() {
    var a: Int
    init {
        a = 3
    }
}

fun box(): String {
    return if (Test().a == 3) "OK" else "fail"
}
