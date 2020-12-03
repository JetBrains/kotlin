fun a() {
    <warning descr="SSR">when(10) {
        in 3..10 -> Unit
        else -> Unit
    }</warning>
}