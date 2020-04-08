fun foo(f: () -> Unit) {
    f()
}

fun main(args: String) {
    foo(<caret>fun() {
        val p1 = 1
        val p2 = 1
    })
}