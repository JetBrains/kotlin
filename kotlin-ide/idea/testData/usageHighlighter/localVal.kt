fun test() {
    val <info descr="null">a</info> = 12
    foo(<info descr="null">~a</info>)
}

fun foo(a: Int) = a

val a = 1