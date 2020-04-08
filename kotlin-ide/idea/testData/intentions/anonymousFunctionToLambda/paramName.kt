fun foo2(f: (Int) -> Unit) {
    f(1)
}

fun main(args: String) {
    foo2(<caret>fun(i) {
        val p = i
    })
}