var foo = 2.also <warning descr="SSR">{}</warning>
val bar1 = 1.also { it.inc() }
val bar2 = 1.also { it -> it.inc() }