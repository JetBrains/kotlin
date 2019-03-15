// EXPECTED_REACHABLE_NODES: 1286
package foo

class A {
    override fun hashCode() = 42
}

class B

fun box(): String {
    if (A().hashCode() != 42) return "Wrong hash 0"

    val o1 = B();
    if (o1.hashCode() != o1.hashCode()) return "Wrong hash 1"

    val o2 = js("\"\"")
    if (o2.hashCode() != o2.hashCode()) return "Wrong hash 2"

    val o3 = js("123")
    if (o3.hashCode() != o3.hashCode()) return "Wrong hash 3"

    val o4 = 123
    if (o4.hashCode() != o4.hashCode()) return "Wrong hash 4"

    val o5 = "123"
    if (o5.hashCode() != o5.hashCode()) return "Wrong hash 5"

    val o6 = (123 as Any)
    if (o6.hashCode() != o6.hashCode()) return "Wrong hash 6"

    return "OK"
}