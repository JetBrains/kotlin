val bar = 20
val foo1 = <warning descr="SSR">"foo"</warning>
val foo2 = <warning descr="SSR">"bar: bar"</warning>
val foo3 = <warning descr="SSR">""</warning>
val foo4 = <warning descr="SSR">"bar: ${ bar + 1 }"</warning>
val foo5 = <warning descr="SSR">"bar: ${ <warning descr="SSR">"$bar"</warning> }"</warning>
val foo6 = <warning descr="SSR">"bar: $bar, bar - 1: ${ bar - 1 }\n"</warning>
val foo7 = <warning descr="SSR">(<warning descr="SSR">"foo"</warning>)</warning>
val foo8 = <warning descr="SSR">(<warning descr="SSR">(<warning descr="SSR">"foo"</warning>)</warning>)</warning>