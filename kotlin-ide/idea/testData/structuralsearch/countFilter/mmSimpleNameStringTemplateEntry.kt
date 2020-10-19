val foo1 = <warning descr="SSR">""</warning>
val foo2 = <warning descr="SSR">"foo"</warning>
val foo3 = <warning descr="SSR">"foo$foo1"</warning>

val bar = "foo$foo1$foo2"