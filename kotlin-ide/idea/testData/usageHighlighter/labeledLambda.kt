fun <R> foo(f: () -> R) = f()

fun test() {
    foo <info descr="null">~bar</info>@ { return@<info descr="null">bar</info> false }
}