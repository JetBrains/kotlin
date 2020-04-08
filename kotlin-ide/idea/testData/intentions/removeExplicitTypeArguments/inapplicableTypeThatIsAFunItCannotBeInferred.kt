// IS_APPLICABLE: false
fun foo() {
    bar<caret><(Int) -> Int>({ baz(it) })
}

fun baz(x: Int): Int = x

fun <T> bar(t: T): Int = 1