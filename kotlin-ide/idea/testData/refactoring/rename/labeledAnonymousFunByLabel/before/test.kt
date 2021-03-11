fun <R> foo(f: () -> R) = f()

fun test() {
    foo (/*rename*/bar@ fun(): Boolean { return@bar false })
}