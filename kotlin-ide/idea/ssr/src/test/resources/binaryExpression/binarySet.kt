val a = intArrayOf(0, 1)

fun b() {
    <warning descr="SSR">a[0] = 1 + 2</warning>
    <warning descr="SSR">a.set(0, 1 + 2)</warning>
    a.set(0, 1)
    a.set(1, 1 + 2)
    val c = intArrayOf(1, 1)
    c.set(0, 1 + 2)
    a[0] = 1
    a[1] = 1 + 2
}