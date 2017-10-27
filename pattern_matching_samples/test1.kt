data class A(val a: Int, val b: Int)

//data class A<T1, T2>(val a: Int, val b: T1, val c: Int, val d: T2)

fun main(args: Array<String>) {
    val (g, k, d) = listOf(1, 2, 3)
    val (a, b, c, e) = listOf(4, 5, 6, 7)
    val n = 10
    val p = Pair(g + k + d, a + b + c + e)
    when (p) {
        match(5, 7) -> {}
        match(m, # a) -> {}
        match(n, #(k + n)) -> {}
        match p @ Pair(:Int, :Int) -> {}
        match(x, y) if (x > a) -> {}
        match("some string ${e} with parameter", _) -> {}
        match x -> {}
    }
}
