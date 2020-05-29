val a = <warning descr="SSR">1 < 2</warning>

val b = <warning descr="SSR">1.compareTo(2) < 0</warning>

val c = 1 < 3

val d = 1.compareTo(3) < 0

val e = 1.compareTo(2) > 0

val f = 2.compareTo(2) < 0

val g = 1.compareTo(2) < 1
