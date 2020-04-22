fun a() {
    var a = 0
    var b = 0
    print(a + b)
    <warning descr="SSR">a = 1
    b = 2</warning>
    print(a + b)
}
