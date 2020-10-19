var foo = 2.also {}
val bar1 = 1.also { it.inc() }
val bar2 = 1.also <warning descr="SSR">{ it -> it.inc() }</warning>