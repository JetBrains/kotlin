fun a(b: (Int) -> Unit = ::println) { b(0) }

fun c() {
    <warning descr="SSR">a()</warning>
    <warning descr="SSR">a() { i -> println("Hello world! $i") }</warning>
    <warning descr="SSR">a { i -> println("Hello world! $i") }</warning>
    <warning descr="SSR">a({ i -> println("Hello world! $i") })</warning>
}