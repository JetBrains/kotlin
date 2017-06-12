// EXPECTED_REACHABLE_NODES: 502
package foo

class A {
    override fun equals(other: Any?) = super.equals(other)
}

fun box(): String {
    val a: A? = null

    testTrue { a == null }
    testFalse { a != null }
    testTrue { null == a }
    testFalse { null != a }

    return "OK"
}
