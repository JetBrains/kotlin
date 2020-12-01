fun foo(x: Any): Any = x.hashCode()

fun a() {
    val i = 0
    <warning descr="SSR">when (foo(i)) {
        is Int -> Unit
        else -> Unit
    }</warning>
    <warning descr="SSR">when (i) {
        1 -> Unit
    }</warning>
}