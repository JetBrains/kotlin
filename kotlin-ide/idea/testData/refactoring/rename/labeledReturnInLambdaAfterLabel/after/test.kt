fun <R> foo(f: () -> R) = f()

fun test() {
    foo {
        return@foo false
    }
}