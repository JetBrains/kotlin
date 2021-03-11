fun <T> run(f: () -> T) = f()

fun foo(): Unit = run {
    bar()
}

fun bar() = 1

fun call(f: () -> Unit) = f()

fun boo(): Unit = call {
    baz()
}

fun baz() {}

fun <T, R> T.let(f: (T) -> R) = f(this)

fun goo(): Unit = 1.let {
    bar()
}