package foo

// CHECK_NOT_CALLED_IN_SCOPE: scope=multiply function=multiply$f

class A(val a: Int)

inline fun <T, R> with2(receiver: T, arg1: R, arg2: R, f: T.(R, R) -> R): R = receiver.f(arg1, arg2)

fun multiply(a: Int, b: Int, c: Int): Int = with2(A(a), b, c) { (x, y) -> a*x*y }

fun box(): String {
    assertEquals(105, multiply(3, 5, 7))

    return "OK"
}