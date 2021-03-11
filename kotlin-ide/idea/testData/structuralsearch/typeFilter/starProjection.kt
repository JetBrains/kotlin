class Foo<T>

<warning descr="SSR">fun foo1(x: Foo<*>) { print(x) }</warning>

fun bar(x: Foo<Int>) { print(x) }