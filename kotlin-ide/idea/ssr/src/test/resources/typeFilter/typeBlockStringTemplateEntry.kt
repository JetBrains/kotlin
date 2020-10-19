val x = 1

val foo = <warning descr="SSR">"${ x.hashCode() }"</warning>
val bar = "${ '$' }"