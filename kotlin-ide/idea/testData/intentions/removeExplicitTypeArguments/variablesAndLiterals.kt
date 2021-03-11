// IS_APPLICABLE: true
fun foo() {
    val x = "1"
    val y = 2
    val z = bar<caret><String, Int, Int, String>(x, 1, y, "x")
}

fun <T, V, R, K> bar(t: T, v: V, r: R, k: K): Int = 2