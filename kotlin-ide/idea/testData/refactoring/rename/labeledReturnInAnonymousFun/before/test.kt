fun <R> foo(f: () -> R) = f()

fun test() {
    foo(fun(): Boolean { return@/*rename*/foo false })
}