val a = arrayOf(0, 1)
val b = <warning descr="SSR">a[0]</warning>
val c = <warning descr="SSR">a[(0)]</warning>
val d = <warning descr="SSR">(a)[0]</warning>
val e = <warning descr="SSR">(a)[(0)]</warning>
val f = <warning descr="SSR">(((a)))[(((0)))]</warning>
val g = a[1]