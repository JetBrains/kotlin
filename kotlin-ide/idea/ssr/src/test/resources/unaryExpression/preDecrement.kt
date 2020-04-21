fun preDecrement(a: Int): Int {
    var b = a
    return <warning descr="SSR">--b</warning>
}