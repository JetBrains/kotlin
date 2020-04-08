// IS_APPLICABLE: true
fun foo() {
    bar(a = 2, b = {<caret> it })
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
