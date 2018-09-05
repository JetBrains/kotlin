// LANGUAGE_VERSION: 1.1
// EXPECTED_REACHABLE_NODES: 1112
package foo

class A() {
    var p = "yeah"
    operator fun mod(other: A): A {
        return A();
    }
}

fun box(): String {
    var c = A()
    val d = c;
    c %= A();
    return if ((c != d) && (c.p == "yeah")) "OK" else "fail"
}