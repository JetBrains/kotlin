package foo

data class A(val n: Int, val m: Int)

class X(val a: A) {
    fun map(f: (A) -> Int) = f(a)
}

fun box(): String {
    val s = (for ((i, j) in X(A(2, 3))) yield i*j).toString()
    return if (s == "6") "OK" else "FAIL: $s"
}