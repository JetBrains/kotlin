fun a() {
    var a = 0
    var b = 0
    print(a + b)
    <warning descr="SSR">a = 1</warning>
    b = 2
    print(a + b)
    a = 1
    print(a + b)
}
