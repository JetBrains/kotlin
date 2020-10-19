fun foo(x: Any): Any = x.hashCode()

fun a() {
    val i = 0
    <warning descr="SSR">when (foo(i)) {
        is Int -> println("ok")
        else -> println("not Int")
    }</warning>
    <warning descr="SSR">when (i) {
        1 -> println("ok")
    }</warning>
}