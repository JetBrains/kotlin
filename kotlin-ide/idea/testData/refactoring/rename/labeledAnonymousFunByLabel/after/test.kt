fun <R> foo(f: () -> R) = f()

fun test() {
    foo (baz@ fun(): Boolean { return@baz false })
}