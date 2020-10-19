class Foo { class Int }

<warning descr="SSR">val foo1: List<Pair<String, (Foo.Int) -> kotlin.Int>> = listOf()</warning>
<warning descr="SSR">val foo2 = listOf("foo" to { _: Foo.Int -> 2 })</warning>

val bar1: List<Pair<String, (Int) -> Int>> = listOf()
val bar2 = listOf("bar" to { _: Int -> 2 })