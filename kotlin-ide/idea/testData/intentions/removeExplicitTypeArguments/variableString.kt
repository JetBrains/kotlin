// IS_APPLICABLE: true
fun foo() {
    val x = "x"
    bar<caret><String>(x)
}

fun <T> bar(t: T): Int = 1