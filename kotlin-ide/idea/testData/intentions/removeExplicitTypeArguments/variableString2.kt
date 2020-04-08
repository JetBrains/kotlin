// IS_APPLICABLE: true
fun foo(x: String) {
    bar<caret><String>(x)
}

fun <T> bar(t: T): Int = 1