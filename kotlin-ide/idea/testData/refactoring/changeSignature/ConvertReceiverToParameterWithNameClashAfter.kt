val x1: Int

class X(val x: Int)

fun foo(x2: X, x: Int): Boolean {
    return x2.x + x > x1
}