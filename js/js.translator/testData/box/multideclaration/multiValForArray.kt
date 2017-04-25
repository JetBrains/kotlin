// EXPECTED_REACHABLE_NODES: 496
package foo

class A {
    operator fun component1(): Int = 1
}
operator fun A.component2(): String = "n"

fun box(): String {
    val list = Array<A>(1, { A() })

    var i = 0;
    var s = ""
    for ((a, b) in list) {
        i = a;
        s = b;
    }

    if (i != 1) return "i != 1, it: " + i
    if (s != "n") return "s != 'n', it: " + s

    return "OK"
}