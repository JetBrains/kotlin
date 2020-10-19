class A { val x = 1 }

val x : A? = null
val y = A()

val foo = <warning descr="SSR">x?.x</warning>
val bar = y.x