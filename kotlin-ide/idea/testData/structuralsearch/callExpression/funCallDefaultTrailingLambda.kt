fun a(b: (Int) -> Int = { it }) { b(0) }

fun c() {
    <warning descr="SSR">a()</warning>
    <warning descr="SSR">a() { i -> i }</warning>
    <warning descr="SSR">a { i -> i }</warning>
    <warning descr="SSR">a({ i -> i })</warning>
}