val foo1: List<Int> = <warning descr="SSR">listOf()</warning>
val foo2 = <warning descr="SSR">listOf(1)</warning>
val foo3 = <warning descr="SSR">listOf(1, 2)</warning>

val bar = listOf(1, 2, 3)