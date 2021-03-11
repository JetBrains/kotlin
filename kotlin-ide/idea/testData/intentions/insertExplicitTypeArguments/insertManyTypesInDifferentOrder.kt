// IS_APPLICABLE: true
fun foo() {
      val z = <caret>bar("1", 1, 2, "x")
}

fun <R, K, T, V> bar(t: T, v: V, r: R, k: K): Int = 2