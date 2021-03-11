fun <R> foo(f: () -> R) = f()

fun test() {
    foo (bar2@ /*rename*/bar@ { return@bar false })
}