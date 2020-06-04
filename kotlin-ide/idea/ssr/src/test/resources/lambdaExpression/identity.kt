val foo: (Int) -> Int = <warning descr="SSR">{ it -> it }</warning>
var bar1: (Int) -> Unit = {}
val bar2: (Int) -> Int = { 1 }
val bar3: (Int) -> Int = { it -> 1 }
