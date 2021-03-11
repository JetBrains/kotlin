val p0: () -> Int = <warning descr="SSR">{ 31 }</warning>
val p1: (Int) -> Int = <warning descr="SSR">{ x -> x }</warning>
val p1b: (Int) -> Int = <warning descr="SSR">{ it }</warning>
val p2: (Int, Int) -> Int = <warning descr="SSR">{ x, y -> x + y }</warning>
val p3: (Int, Int, Int) -> Int = { x, y, z -> x + y + z }