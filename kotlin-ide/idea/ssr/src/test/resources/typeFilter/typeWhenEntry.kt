val x = 1

val y = <warning descr="SSR">when(x) {
    2 -> 2
    else -> 0
}</warning>

val z = when(x) {
    2 -> "2"
    else -> ""
}