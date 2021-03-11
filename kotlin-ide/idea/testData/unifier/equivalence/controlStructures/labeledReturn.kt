inline fun run(f: () -> Unit) = f()

fun foo(a: Int) {
    run {
        run {
            if (a > 0) <selection>return@foo</selection>
            if (a < 0) return
            return@foo
        }
    }
}