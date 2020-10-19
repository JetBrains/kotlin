annotation class Foo()
annotation class Bar(val x: String)


<warning descr="SSR">@Foo
fun foo1() { }</warning>

<warning descr="SSR">@Foo
@Bar("bar")
fun foo2() { }</warning>

fun bar() { }