<warning descr="SSR">fun foo(foo: Array<out Any>) { print(foo) }</warning>

fun bar1(foo: Array<Any>) { print(foo) }
fun bar2(foo: Array<in Any>) { print(foo) }