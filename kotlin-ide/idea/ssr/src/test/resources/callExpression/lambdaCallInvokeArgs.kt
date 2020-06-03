val a: (Int, Int) -> Unit = { i, j -> }

fun b() {
    <warning descr="SSR">a(0, 0)</warning>
    <warning descr="SSR">a.invoke(0, 0)</warning>
}
