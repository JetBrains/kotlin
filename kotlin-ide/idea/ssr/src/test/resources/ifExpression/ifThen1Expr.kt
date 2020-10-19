fun a() {
    var b = false
    <warning descr="SSR">if (true) b = true</warning>
    <warning descr="SSR">if (true) {
        b = true
    }</warning>
    if (true) {
        b = true
        print(b)
    }
    print(b)
}

