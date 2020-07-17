fun a(i: Int, b: () -> String) {
    // prevent unused warning
    println(i)
    b()

    <warning descr="SSR">a(2, {"foo"})</warning>
    <warning descr="SSR">a(2) {"foo"}</warning>
}