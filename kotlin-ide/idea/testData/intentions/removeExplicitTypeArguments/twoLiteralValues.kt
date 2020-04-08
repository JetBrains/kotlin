// IS_APPLICABLE: true
fun foo() {
    val x = bar<caret><String, Int>("x", 0)
}

fun <T, V> bar(t: T, v: V): Int = 1