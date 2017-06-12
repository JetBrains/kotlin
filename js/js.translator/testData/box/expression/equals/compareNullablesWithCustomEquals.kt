// EXPECTED_REACHABLE_NODES: 524
package foo

class A {
    override fun equals(other: Any?) = this === other
}

fun box(): String {
    val a: A? = null
    val b: A? = null
    val c: A? = A()
    val d: A? = A()
    val e: A = A()

    // compare nullable vals with null
    testTrue { a == b }
    testTrue { a == a }
    testFalse { a != b }
    testFalse { a != a }

    // compare null and non-null inside nullable vals
    testFalse { a == c }
    testTrue { a != c }
    testFalse { c == a }
    testTrue { c != a }

    // compare nullables vals with non-null
    testFalse { c == d }
    testTrue { c == c }
    testTrue { c != d }
    testFalse { d == c }
    testTrue { d != c }
    testFalse { d != d }

    // compare nullable val with null with non-nullable
    testFalse { a == e }
    testTrue { a != e }
    testFalse { e == a }
    testTrue { e != a }

    // compare nullable val with non-null with non-nullable
    testFalse { c == e }
    testTrue { c != e }
    testFalse { e == c }
    testTrue { e != c }

    return "OK"
}
