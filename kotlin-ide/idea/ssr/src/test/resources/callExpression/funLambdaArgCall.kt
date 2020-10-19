fun a(b: () -> Unit) {
    b.invoke()
}

fun c() {
    <warning descr="SSR">a { println() }</warning>
}