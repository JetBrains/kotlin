fun <R> foo(f: () -> R) = f()

fun test() {
    foo bar@ { return@bar  false }
}