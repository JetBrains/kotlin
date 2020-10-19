annotation class Foo(val bar: IntArray)

<warning descr="SSR">@Foo([1, 2, 3, 4])</warning>
fun a() { }

<warning descr="SSR">@Foo(intArrayOf(1, 2, 3, 4))</warning>
fun b() { }