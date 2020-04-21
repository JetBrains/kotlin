fun preIncrement(a: Int): Int {
    var b = a
    return <warning descr="SSR">++b</warning>
}