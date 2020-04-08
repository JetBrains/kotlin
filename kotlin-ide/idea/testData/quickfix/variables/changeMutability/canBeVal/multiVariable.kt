// "Change to val" "true"
fun foo(p: Int) {
    <caret>var (v1, v2) = getPair()!!
    v1
}

fun getPair(): Pair<Int, String>? = null

data class Pair<T1, T2>(val a: T1, val b: T2)