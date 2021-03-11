fun <R> foo(f: () -> R) = f()

fun test() {
    foo (<info descr="null">~bar</info>@ fun(): Boolean { return@<info descr="null">bar</info> false })
}