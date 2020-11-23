val p0: () -> Int = { 31 }
val p1: (Int) -> Int = <warning descr="SSR">{ x -> x }</warning>
val p1b: (Int) -> Int = <warning descr="SSR">{ it }</warning>
val p2: (Int, Int) -> Int = { x, y -> x + y }
val p3: (Int, Int, Int) -> Int = { x, y, z -> x + y + z }