fun foo1 (bar: Int) { print(bar.hashCode()) }
fun foo2 (bar: kotlin.Int) { print(bar.hashCode()) }
<warning descr="SSR">fun foo3 (bar: Int?) { print(bar.hashCode()) }</warning>
fun foo4 (bar: (Int) -> Int) { print(bar.hashCode()) }