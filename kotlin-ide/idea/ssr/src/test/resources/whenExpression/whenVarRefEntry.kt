fun a() {
    val i = 0
    <warning descr="SSR">when (i) {
        1 -> println("one")
    }</warning>
}