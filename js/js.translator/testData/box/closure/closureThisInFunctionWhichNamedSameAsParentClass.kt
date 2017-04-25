// EXPECTED_REACHABLE_NODES: 495
package foo

class B(val b: String)

class A(val a: String) {
    fun B.A() = { a + b }

    fun foo(a: B) = a.A()()
}

fun box(): String {
    val bar = A("bar")
    val baz = B("baz")

    val r = bar.foo(baz)
    if (r != "barbaz") return "$r";

    return "OK"
}