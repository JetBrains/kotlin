// IS_APPLICABLE: false
fun test() {
    val i = 1
    foo(baz<caret>(i, 2))
}

fun foo(i: Int) {}

fun baz(i: Int, j: Int) = 1
