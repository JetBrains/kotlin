fun <R> foo(f: () -> R) = f()

fun test() {
    foo(fun(): Boolean { return@foo/*rename*/ false })
}