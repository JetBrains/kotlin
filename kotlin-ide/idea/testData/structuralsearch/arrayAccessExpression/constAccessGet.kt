val a = arrayOf(0, 1)

val b = <warning descr="SSR">a[0]</warning>

val c = <warning descr="SSR">a.get(0)</warning>

val d = a.get(1)

val e = arrayOf(1, 1)

val f = e.get(0)

val g = a.set(0, 0)