fun a(vararg b: Int = intArrayOf(0)) { println(b) }

fun c() {
    <warning descr="SSR">a()</warning>
    <warning descr="SSR">a(0)</warning>
    <warning descr="SSR">a(10, 0, 3)</warning>
    <warning descr="SSR">a(*intArrayOf(1, 2, 3))</warning>
    <warning descr="SSR">a(0, *intArrayOf(1, 2, 3), 4)</warning>
}