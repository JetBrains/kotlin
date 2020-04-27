fun a() {
    return
}

fun b() {
    <warning descr="SSR">a()</warning>
}