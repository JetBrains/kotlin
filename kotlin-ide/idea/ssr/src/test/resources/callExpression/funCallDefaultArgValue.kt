fun a(b: Int = 0) { println(b) }

fun c() {
    <warning descr="SSR">a()</warning>
    <warning descr="SSR">a(0)</warning>
    a(1)
}