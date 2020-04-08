fun <R> foo(f: () -> R) = f()

fun test() {
    foo {
        return@/*rename*/foo false
    }
}