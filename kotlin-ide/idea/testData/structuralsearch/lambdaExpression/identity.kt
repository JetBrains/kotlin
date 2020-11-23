val foo: (Int) -> Int = <warning descr="SSR">{ it -> it }</warning>
val foo2: (Int) -> Int = <warning descr="SSR">{ 1 }</warning>
var bar1: (Int) -> Unit = {}
val bar2: () -> Int = { 1 }
val bar3: (Int) -> Int = { it -> 1 }
