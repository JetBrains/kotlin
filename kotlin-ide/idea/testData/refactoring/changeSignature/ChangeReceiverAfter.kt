fun String.foo(x: X, k: Int): Boolean {
    return x.k + length - k > 0
}

class X(val k: Int)

fun test() {
    "1".foo(X(0), 2)
}