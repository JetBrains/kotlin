fun a(b: Int, c: Int = 0) {
    println(b) // prevent unused warning
    println(c) // prevent unused warning
}

fun d() {
    <warning descr="SSR">a(0)</warning>
    <warning descr="SSR">a(1)</warning>
    <warning descr="SSR">a(1, 0)</warning>
    <warning descr="SSR">a(1, 1)</warning>
}