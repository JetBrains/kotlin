// EXPECTED_REACHABLE_NODES: 1285
package foo

// CHECK_CONTAINS_NO_CALLS: myMultiply except=A;imul;new_foo_A_eb0rqk_k$

internal class A(val a: Int)

internal inline fun <T, R> with2(receiver: T, arg1: R, arg2: R, f: T.(R, R) -> R): R = receiver.f(arg1, arg2)

// CHECK_BREAKS_COUNT: function=myMultiply count=0 TARGET_BACKENDS=JS_IR,JS_IR_ES6
// CHECK_LABELS_COUNT: function=myMultiply name=$l$block count=0 TARGET_BACKENDS=JS_IR,JS_IR_ES6
internal fun myMultiply(a: Int, b: Int, c: Int): Int = with2(A(a), b, c) { x, y -> a*x*y }

fun box(): String {
    assertEquals(105, myMultiply(3, 5, 7))

    return "OK"
}
