val foo = 2
val bar = <warning descr="SSR">"$foo + 1 = ${"${foo + 1}"}"</warning>
val bar2 = "$foo + 1 = ${foo + 1}"