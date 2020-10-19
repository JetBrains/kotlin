class Foo { class Int }

<warning descr="SSR">fun foo1 (bar: Int = 1) { print(bar.hashCode()) }</warning>
<warning descr="SSR">fun foo2 (bar: kotlin.Int) { print(bar.hashCode()) }</warning>
fun foo3 (bar: Int?) { print(bar.hashCode()) }
fun foo4 (bar: (Int) -> Int) { print(bar.hashCode()) }
fun foo5 (bar: Foo.Int) { print(bar.hashCode()) }
