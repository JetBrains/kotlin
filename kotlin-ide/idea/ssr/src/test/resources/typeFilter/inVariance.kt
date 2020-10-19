<warning descr="SSR">fun foo(foo: Array<in String>) { print(foo) }</warning>

fun bar1(foo: Array<String>) { print(foo) }
fun bar2(foo: Array<out String>) { print(foo) }