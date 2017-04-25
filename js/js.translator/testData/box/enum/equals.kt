// EXPECTED_REACHABLE_NODES: 537
package foo

enum class Foo {
    A,
    B
}

fun box(): String {
    testFalse { Foo.A == Foo.B }
    testTrue { Foo.A != Foo.B }
    testTrue { Foo.A == Foo.A }
    testFalse { Foo.A != Foo.A }

    testFalse { Foo.A == null }
    testTrue { Foo.A != null }
    testFalse { null == Foo.A }
    testTrue { null != Foo.A }

    return "OK"
}
