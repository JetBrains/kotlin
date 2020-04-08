// IS_APPLICABLE: true
val x = "x"

fun foo() {
    bar<caret><String>(x)
}

fun <T> bar(t: T): Int = 1