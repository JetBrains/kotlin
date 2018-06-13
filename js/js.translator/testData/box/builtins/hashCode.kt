// EXPECTED_REACHABLE_NODES: 1117
package foo

class A {
    override fun hashCode() = 42
}

class B

fun box(): String {
    if (A().hashCode() != 42) return "Wrong hash"
    B().hashCode()
    js("\"\"").hashCode()
    js("123").hashCode()
    123.hashCode()
    "123".hashCode()
    (123 as Any).hashCode()
    return "OK"
}