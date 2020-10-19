val a = { }

fun b() {
    <warning descr="SSR">a()</warning>
    <warning descr="SSR">a.invoke()</warning>
}
