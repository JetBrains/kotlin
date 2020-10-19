<warning descr="SSR">val foo = { x: String -> x.hashCode() }</warning>

val bar1 = { x: Int -> x.hashCode() }

val bar2 = { x: Int -> "$x" }