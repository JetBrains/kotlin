fun foo() {
    var a = 0
    var b = 0
    <warning descr="SSR">if (true) {
        a = 1
    }</warning>

    if (true) {
        b = 2
    }
    <warning descr="SSR">if (true) a = 1</warning>
    println(a + b)
}

