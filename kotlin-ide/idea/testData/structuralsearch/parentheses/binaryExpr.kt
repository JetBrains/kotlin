val a = <warning descr="SSR">1 + 2 + 3</warning>
val b = <warning descr="SSR">(1 + 2) + 3</warning>
val c = 1 + (2 + 3)
val d = <warning descr="SSR">(<warning descr="SSR">1 + 2 + 3</warning>)</warning>