package foo

// CHECK_CONTAINS_NO_CALLS: test
// CHECK_VARS_COUNT: function=test count=0

class A(val x: Int)

inline fun A.run(fn: A.()->Int) = fn()

fun test(a: A): Int = a.run { x }

fun box(): String {
    assertEquals(1, test(A(1)))
    assertEquals(2, test(A(2)))

    return "OK"
}