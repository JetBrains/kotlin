class X(val k: Int)

fun foo(abc: X, n: Int): Boolean {
    return abc.k > n
}