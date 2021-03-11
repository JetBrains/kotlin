val bar = 20
val foo1 = "foo"
val foo2 = "bar: bar"
val foo3 = ""
val foo4 = <warning descr="SSR">"bar: ${ bar + 1 }"</warning>
val foo5 = <warning descr="SSR">"bar: ${ <warning descr="SSR">"${ bar }"</warning> }"</warning>
val foo6 = <warning descr="SSR">"bar: $bar, bar - 1: ${ bar - 1 }"</warning>