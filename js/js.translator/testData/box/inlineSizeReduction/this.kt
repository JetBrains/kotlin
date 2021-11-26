// EXPECTED_REACHABLE_NODES: 1288
package foo

// CHECK_CONTAINS_NO_CALLS: test

// FIXME: The IR backend generates a lot of redundant vars
// CHECK_VARS_COUNT: function=test count=0 TARGET_BACKENDS=JS

internal class A(val x: Int) {
    inline fun f(): Int = x

    inline fun ff(): Int = f()
}

internal fun test(a: A): Int = a.ff()

fun box(): String {
    assertEquals(1, test(A(1)))
    assertEquals(2, test(A(2)))

    return "OK"
}