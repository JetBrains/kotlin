val a = arrayOf(0, 1)
val b = <warning descr="SSR">a[0]</warning>
val c = a[1]