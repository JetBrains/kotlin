val a = <warning descr="SSR">1 in 0..2</warning>
val b = <warning descr="SSR">(0..2).contains(1)</warning>
val c = (0..1).contains(1)
val d = (0..2).contains(2)
val e = 2 in 0..2
val f = 1 in 1..2
val g = 1 !in 0..2
val h = !(0..2).contains(1)