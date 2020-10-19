annotation class Foo
annotation class Bar

<warning descr="SSR">@Foo</warning> val foo1 = 1

@Bar val bar1 = 1

<warning descr="SSR">@Foo</warning> fun foo2(): Int = 1

@Bar fun bar2(): Int = 2